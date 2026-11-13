import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../services/api.service';
import { errorMessage,fmtDate,daysBetween } from '../shared';

@Component({selector:'app-planning',standalone:true,imports:[CommonModule,FormsModule],template:`
<section class="panel">
  <div class="section-head"><div><h2>Carta Gantt interactiva</h2><p>Planificación con días hábiles, cuatro dependencias y detección de ciclos.</p></div>
   <div class="toolbar"><label>Escala <select [(ngModel)]="scale"><option [ngValue]="3">Compacta</option><option [ngValue]="6">Diaria</option><option [ngValue]="10">Ampliada</option></select></label><button class="outline" (click)="load()">Actualizar</button></div></div>
  <div class="filters"><label>Etapa <select [(ngModel)]="filterStage"><option value="">Todas</option><option *ngFor="let x of stages()" [value]="x.codigo">{{x.codigo}} {{x.nombre}}</option></select></label>
  <label>Responsable <input [(ngModel)]="filterPerson" placeholder="Buscar"></label><span class="hint">Azul: planificación · Verde: avance · Rojo: atraso · ◆: hito</span></div>
  <p *ngIf="error" class="error">{{error}}</p><p *ngIf="success" class="success">{{success}}</p>
  <div class="gantt-scroll">
    <div class="gantt-layout">
      <div class="gantt-labels"><div class="gantt-ruler">EDT / Actividad</div>
        <div class="gantt-label" *ngFor="let r of rows(); let i=index" [class.major]="r.tipo==='ETAPA'">
          <span [style.padding-left.px]="r.tipo==='ETAPA'?6:r.tipo==='ACTIVIDAD'?17:29">{{r.tipo==='HITO'?'◆ ':''}}{{r.codigo}} {{r.nombre||r.descripcion}}</span>
          <button *ngIf="admin&&r.tipo==='SUBACTIVIDAD'" class="edit-mini" (click)="selectItem(r)">✎</button>
        </div>
      </div>
      <div class="gantt-chart" [style.width.px]="chartWidth()">
        <div class="gantt-ruler" [style.width.px]="chartWidth()"><span *ngFor="let t of ticks()" [style.left.px]="x(t.date)">{{t.label}}</span></div>
        <div class="gantt-grid" [style.height.px]="rows().length*36" [style.width.px]="chartWidth()">
          <div class="gantt-background-row" *ngFor="let r of rows()" [class.row-stage]="r.tipo==='ETAPA'"></div>
          <div *ngFor="let t of ticks()" class="gantt-gridline" [style.left.px]="x(t.date)"></div>
          <ng-container *ngFor="let r of rows(); let i=index">
            <div *ngIf="r.tipo!=='HITO'" class="gantt-bar" [class.stage-bar]="r.tipo==='ETAPA'" [class.activity-bar]="r.tipo==='ACTIVIDAD'"
              [class.overdue]="late(r)" [style.top.px]="i*36+10" [style.left.px]="x(r.inicio)" [style.width.px]="barWidth(r)" [title]="r.codigo+' · '+r.inicio+' → '+r.fin+' · '+r.progreso+'%'">
              <div class="gantt-done" [style.width.%]="r.progreso"></div>
            </div>
            <div *ngIf="r.tipo==='HITO'" class="milestone-mark" [style.left.px]="x(r.fecha_comprometida)" [style.top.px]="i*36+8" [title]="r.descripcion+' '+r.fecha_comprometida">◆</div>
          </ng-container>
          <svg class="gantt-links" [attr.width]="chartWidth()" [attr.height]="rows().length*36">
            <defs><marker id="arrowhead" markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto"><path d="M0 0 L8 4 L0 8 z" fill="#69798a"/></marker></defs>
            <ng-container *ngFor="let e of dependencies">
              <path *ngIf="line(e) as l" [attr.d]="l" stroke="#69798a" stroke-width="1.4" fill="none" stroke-dasharray="3 2" marker-end="url(#arrowhead)"></path>
            </ng-container>
          </svg>
        </div>
      </div>
    </div>
  </div>
</section>
<section *ngIf="admin" class="panel editor">
  <h2>Modificar duración y fechas</h2>
  <p class="subtle">La fecha base se mantiene como referencia. Las sucesoras se recalculan automáticamente por precedencias.</p>
  <div class="form-grid"><label>Subactividad<select [(ngModel)]="selectedId" (ngModelChange)="selectedChanged()"><option [ngValue]="0">Selecciona</option><option *ngFor="let x of leaves()" [ngValue]="x.id">{{x.codigo}} {{x.nombre}}</option></select></label>
   <label>Duración (días hábiles)<input type="number" min="1" [(ngModel)]="duration"></label>
   <label>Fecha base<input type="date" [(ngModel)]="baseStart"></label>
   <div class="form-actions"><button class="primary" (click)="saveDuration()" [disabled]="!selectedId">Guardar y recalcular</button></div>
  </div>
</section>
<section class="panel" *ngIf="admin">
  <h2>Dependencias y desfases</h2>
  <p class="subtle">FS: fin-inicio · SS: inicio-inicio · FF: fin-fin · SF: inicio-fin. Desfase en días hábiles (puede ser negativo).</p>
  <div class="form-grid">
   <label>Predecesora<select [(ngModel)]="newDep.predecesora_id"><option *ngFor="let x of leaves()" [ngValue]="x.id">{{x.codigo}} {{x.nombre}}</option></select></label>
   <label>Sucesora<select [(ngModel)]="newDep.sucesora_id"><option *ngFor="let x of leaves()" [ngValue]="x.id">{{x.codigo}} {{x.nombre}}</option></select></label>
   <label>Tipo<select [(ngModel)]="newDep.tipo"><option *ngFor="let s of ['FS','SS','FF','SF']">{{s}}</option></select></label>
   <label>Desfase (+/-)<input type="number" [(ngModel)]="newDep.desfase"></label>
   <div class="form-actions span-2"><button class="primary" (click)="addDependency()">Agregar dependencia</button></div>
  </div>
  <div class="table-wrap"><table><thead><tr><th>Predecesora</th><th>Relación</th><th>Sucesora</th><th>Desfase</th><th></th></tr></thead><tbody>
    <tr *ngFor="let d of dependencies"><td>{{d.codigo_predecesora}}</td><td><span class="tag">{{d.tipo}}</span></td><td>{{d.codigo_sucesora}}</td><td>{{d.desfase}} días</td>
      <td><button class="small danger-outline" (click)="removeDependency(d)">Eliminar</button></td></tr>
  </tbody></table></div>
  <h3>Calendario de días no laborables</h3><div class="toolbar"><input type="date" [(ngModel)]="newHoliday"><button class="outline" (click)="addHoliday()">Agregar feriado</button></div>
  <div class="chips"><span class="tag pale" *ngFor="let d of holidays">{{fmtDate(d.fecha)}} <button (click)="removeHoliday(d)">×</button></span></div>
</section>`})
export class PlanningComponent implements OnInit {
 @Input() admin=false;
 data:any={items:[],dependencias:[],hitos:[]};dependencies:any[]=[];holidays:any[]=[];
 error='';success='';scale=3;filterStage='';filterPerson='';selectedId=0;duration=5;baseStart='';newHoliday='';
 newDep:any={proyecto_id:1,predecesora_id:null,sucesora_id:null,tipo:'FS',desfase:0};fmtDate=fmtDate;
 constructor(private api:ApiService){}
 ngOnInit(){this.load();}
 load(){this.api.get(`/gantt?proyectoId=${this.api.projectId}`).subscribe({next:(v:any)=>{this.data=v;this.dependencies=v.dependencias;if(!this.newDep.predecesora_id&&this.leaves().length>1){this.newDep.predecesora_id=this.leaves()[0].id;this.newDep.sucesora_id=this.leaves()[1].id;}},error:e=>this.error=errorMessage(e)});
   this.api.get(`/calendario?proyectoId=${this.api.projectId}`).subscribe({next:v=>this.holidays=v,error:e=>this.error=errorMessage(e)});}
 leaves(){return (this.data.items||[]).filter((x:any)=>x.tipo==='SUBACTIVIDAD');}
 stages(){return (this.data.items||[]).filter((x:any)=>x.tipo==='ETAPA');}
 rows(){let items=(this.data.items||[]).filter((x:any)=>(!this.filterStage||x.codigo.startsWith(this.filterStage))&&(!this.filterPerson||x.responsable.toLowerCase().includes(this.filterPerson.toLowerCase())));
   let milestones=(this.data.hitos||[]).filter((x:any)=>!this.filterStage&&!this.filterPerson).map((x:any)=>({...x,tipo:'HITO',codigo:x.codigo,nombre:x.descripcion}));return [...items,...milestones];}
 startDate(){const dates=(this.data.items||[]).map((x:any)=>x.inicio).filter(Boolean).sort();return dates[0]||'2026-10-05';}
 chartWidth(){const dates=[...(this.data.items||[]).map((x:any)=>x.fin),...(this.data.hitos||[]).map((x:any)=>x.fecha_comprometida)].filter(Boolean).sort();return Math.max(850, Math.min(3600,(daysBetween(this.startDate(),dates[dates.length-1]||'2027-07-02')+35)*this.scale));}
 x(date:string){return Math.max(4,daysBetween(this.startDate(),date)*this.scale+6);}
 barWidth(r:any){return Math.max(this.scale*2,(daysBetween(r.inicio,r.fin)+1)*this.scale);}
 ticks(){const result:{date:string,label:string}[]=[];const start=new Date(this.startDate()+'T12:00:00');let day=new Date(start);
  while(daysBetween(this.startDate(),day.toISOString().substring(0,10))*this.scale<this.chartWidth()-60){result.push({date:day.toISOString().substring(0,10),label:day.toLocaleDateString('es-CL',{month:'short',year:'2-digit'})});day.setMonth(day.getMonth()+1);day.setDate(1);if(result.length>24)break;}return result;}
 late(r:any){return r.tipo==='SUBACTIVIDAD'&&r.progreso<100&&new Date(r.fin+'T23:59:59').getTime()<Date.now();}
 line(e:any){const rows=this.rows();const p=rows.find((x:any)=>x.id===e.predecesora_id&&x.tipo==='SUBACTIVIDAD');const s=rows.find((x:any)=>x.id===e.sucesora_id&&x.tipo==='SUBACTIVIDAD');if(!p||!s)return '';
  const y1=rows.indexOf(p)*36+17,y2=rows.indexOf(s)*36+17;
  const left=e.tipo==='SS'||e.tipo==='SF'?this.x(p.inicio):this.x(p.fin)+this.scale;
  const right=e.tipo==='FF'||e.tipo==='SF'?this.x(s.fin)+this.scale:this.x(s.inicio);
  return `M ${left} ${y1} L ${left+9} ${y1} L ${left+9} ${y2} L ${right} ${y2}`;
 }
 selectItem(x:any){this.selectedId=x.id;this.selectedChanged();}
 selectedChanged(){const x=this.leaves().find((i:any)=>i.id===this.selectedId);if(x){this.duration=x.duracion;this.baseStart=x.inicio_base;}}
 saveDuration(){this.error='';this.api.put('/edt/'+this.selectedId,{duracion:this.duration,inicio_base:this.baseStart}).subscribe({next:()=>{this.success='Planificación y sucesoras recalculadas';this.load();},error:e=>this.error=errorMessage(e)});}
 addDependency(){this.error='';this.newDep.proyecto_id=this.api.projectId;this.api.post('/dependencias',this.newDep).subscribe({next:()=>{this.success='Dependencia registrada';this.load();},error:e=>this.error=errorMessage(e)});}
 removeDependency(d:any){this.api.delete('/dependencias/'+d.id).subscribe({next:()=>this.load(),error:e=>this.error=errorMessage(e)});}
 addHoliday(){if(!this.newHoliday)return;this.api.post('/calendario',{proyecto_id:this.api.projectId,fecha:this.newHoliday,descripcion:'No laborable'}).subscribe({next:()=>{this.newHoliday='';this.load();},error:e=>this.error=errorMessage(e)});}
 removeHoliday(h:any){this.api.delete('/calendario/'+h.id).subscribe({next:()=>this.load(),error:e=>this.error=errorMessage(e)});}
}
