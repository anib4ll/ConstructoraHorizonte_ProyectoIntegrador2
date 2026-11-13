import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../services/api.service';
import { errorMessage,fmtDate,fmtMoney } from '../shared';

@Component({selector:'app-changes',standalone:true,imports:[CommonModule,FormsModule],template:`
<section class="panel"><div class="section-head"><div><h2>Solicitudes de cambio</h2><p>Identificación, evaluación, aprobación, diez etapas de seguimiento y línea base de modificaciones.</p></div><button *ngIf="admin" class="primary" (click)="create()">+ Solicitud</button></div>
 <p *ngIf="error" class="error">{{error}}</p><p *ngIf="message" class="success">{{message}}</p>
 <div class="filters"><label>Estado<select [(ngModel)]="filterState"><option value="">Todos</option><option *ngFor="let s of states">{{s}}</option></select></label><label>Riesgo<select [(ngModel)]="filterRisk"><option value="">Todos</option><option>BAJO</option><option>MEDIO</option><option>ALTO</option><option>CRITICO</option></select></label></div>
 <div class="table-wrap"><table><thead><tr><th>Código</th><th>Solicitud</th><th>Tipo</th><th>Prioridad / riesgo</th><th>Estado</th><th>Estimación</th><th></th></tr></thead>
 <tbody><tr *ngFor="let c of visible()"><td>{{c.codigo}}</td><td>{{c.descripcion}}</td><td>{{c.tipo}}</td><td>{{c.prioridad}} / {{c.riesgo}}</td><td><span class="tag">{{c.estado}}</span></td><td>{{fmtMoney(c.costo_estimado)}}</td><td><button class="small outline" (click)="select(c)">Detalle</button></td></tr></tbody></table></div>
</section>
<section *ngIf="selected" class="panel"><div class="section-head"><div><h2>{{selected.codigo}} — Seguimiento</h2><p>{{selected.descripcion}}</p></div><button class="outline" (click)="selected=null">Cerrar detalle</button></div>
 <div class="cards"><article class="mini-card"><h3>Información de la solicitud</h3><p><strong>Solicitante:</strong> {{selected.solicitante}}</p><p><strong>Justificación:</strong> {{selected.justificacion}}</p><p><strong>Impacto:</strong> {{selected.impacto}}</p><p><strong>Fecha compromiso:</strong> {{fmtDate(selected.fecha_comprometida)}}</p><p><strong>Resultado:</strong> {{selected.resultado||'Pendiente'}}</p></article>
  <article class="mini-card"><h3>Modificaciones propuestas</h3><p>Subactividad: {{selected.item_id||'Sin modificación'}}</p><p>Nueva duración: {{selected.nueva_duracion||'No aplica'}}</p><p>Asignación: {{selected.asignacion_id||'Sin modificación'}}</p><p>Nueva cantidad: {{selected.nueva_cantidad||'No aplica'}}</p><p>Estimado: {{fmtMoney(selected.costo_estimado)}}</p>
   <div *ngIf="admin && (selected.estado==='ABIERTO'||selected.estado==='EN_EVALUACION')" class="toolbar"><button class="primary" (click)="approve()">Aprobar y aplicar</button><button class="danger-outline" (click)="reject()">Rechazar</button><button class="outline" (click)="edit(selected)">Editar</button></div>
  </article></div>
 <h3>Etapas del cambio</h3><p class="subtle">Se debe completar la secuencia de diez etapas antes del cierre.</p>
 <div class="stages"><div *ngFor="let s of stages" class="stage-row"><span class="stage-number">{{s.orden}}</span><div><strong>{{s.nombre}}</strong><small class="block subtle">{{s.evidencia||'Sin evidencia'}} {{s.fecha?(' — '+fmtDate(s.fecha)):''}}</small></div><span class="tag" [class.good]="s.completada">{{s.completada?'Completada':'Pendiente'}}</span><button *ngIf="admin&&!s.completada" class="small outline" (click)="completeStage(s)">Completar</button></div></div>
 <h3>Línea base del cambio</h3><p *ngIf="!baseline.length" class="subtle">Se registrará al aprobar una solicitud con modificaciones.</p>
 <div *ngIf="baseline.length" class="table-wrap"><table><thead><tr><th>Campo</th><th>Antes</th><th>Después</th><th>Fecha</th></tr></thead><tbody><tr *ngFor="let b of baseline"><td>{{b.campo}}</td><td>{{b.antes}}</td><td>{{b.despues}}</td><td>{{b.fecha}}</td></tr></tbody></table></div>
 <div *ngIf="admin&&(selected.estado==='APROBADO'||selected.estado==='EN_IMPLEMENTACION')" class="editor"><h3>Cerrar solicitud</h3><div class="form-grid"><label>Costo real<input type="number" [(ngModel)]="closeForm.costo_real"></label><label>Adopción (%)<input type="number" [(ngModel)]="closeForm.adopcion"></label><label>Efectividad (%)<input type="number" [(ngModel)]="closeForm.efectividad"></label><label class="span-2">Resultado<textarea [(ngModel)]="closeForm.resultado"></textarea></label><div class="form-actions"><button class="primary" (click)="close()">Cerrar cambio</button></div></div></div>
</section>
<section *ngIf="editing&&admin" class="panel editor"><h2>{{form.id?'Editar solicitud':'Nueva solicitud'}}</h2>
 <form (ngSubmit)="save()" class="form-grid"><label *ngIf="!form.id">Código<input name="codigo" [(ngModel)]="form.codigo" required></label>
 <label *ngIf="!form.id">Solicitante<input name="solicitante" [(ngModel)]="form.solicitante" required></label>
 <label>Descripción<input name="descripcion" [(ngModel)]="form.descripcion" required></label>
 <label>Tipo<input name="tipo" [(ngModel)]="form.tipo" required></label>
 <label>Prioridad<select name="prioridad" [(ngModel)]="form.prioridad"><option>BAJA</option><option>MEDIA</option><option>ALTA</option><option>CRITICA</option></select></label>
 <label>Riesgo<select name="riesgo" [(ngModel)]="form.riesgo"><option>BAJO</option><option>MEDIO</option><option>ALTO</option><option>CRITICO</option></select></label>
 <label>Responsable<input name="responsable" [(ngModel)]="form.responsable"></label>
 <label>Fecha comprometida<input type="date" name="fecha_comprometida" [(ngModel)]="form.fecha_comprometida"></label>
 <label>Costo estimado<input type="number" min="0" name="costo_estimado" [(ngModel)]="form.costo_estimado"></label>
 <label>Subactividad afectada<select name="item_id" [(ngModel)]="form.item_id"><option [ngValue]="null">No modifica duración</option><option *ngFor="let x of leaves" [ngValue]="x.id">{{x.codigo}} {{x.nombre}}</option></select></label>
 <label>Nueva duración<input type="number" min="1" name="nueva_duracion" [(ngModel)]="form.nueva_duracion"></label>
 <label>Asignación afectada<select name="asignacion_id" [(ngModel)]="form.asignacion_id"><option [ngValue]="null">No modifica recursos</option><option *ngFor="let a of assignments" [ngValue]="a.id">{{a.codigo_actividad}} · {{a.recurso}}</option></select></label>
 <label>Nueva cantidad<input type="number" min="0" name="nueva_cantidad" [(ngModel)]="form.nueva_cantidad"></label>
 <label class="span-2">Justificación<textarea name="justificacion" [(ngModel)]="form.justificacion"></textarea></label>
 <label class="span-2">Impacto<textarea name="impacto" [(ngModel)]="form.impacto"></textarea></label>
 <div class="form-actions span-2"><button class="primary">Guardar solicitud</button><button type="button" class="outline" (click)="editing=false">Cancelar</button></div></form>
</section>`})
export class ChangesComponent implements OnInit{
 @Input() admin=false;
 changes:any[]=[];stages:any[]=[];baseline:any[]=[];leaves:any[]=[];assignments:any[]=[];
 selected:any=null;form:any={};closeForm:any={costo_real:0,adopcion:85,efectividad:85,resultado:'Cambio aceptado'};
 editing=false;filterState='';filterRisk='';error='';message='';fmtDate=fmtDate;fmtMoney=fmtMoney;
 states=['ABIERTO','EN_EVALUACION','APROBADO','RECHAZADO','EN_IMPLEMENTACION','CERRADO'];
 constructor(private api:ApiService){}
 ngOnInit(){this.load();}
 load(){this.api.get(`/cambios?proyectoId=${this.api.projectId}`).subscribe({next:v=>this.changes=v,error:e=>this.error=errorMessage(e)});
 this.api.get(`/edt?proyectoId=${this.api.projectId}`).subscribe({next:(v:any)=>this.leaves=v.filter((x:any)=>x.tipo==='SUBACTIVIDAD'),error:e=>this.error=errorMessage(e)});
 this.api.get(`/asignaciones?proyectoId=${this.api.projectId}`).subscribe({next:v=>this.assignments=v,error:e=>this.error=errorMessage(e)});}
 visible(){return this.changes.filter(c=>(!this.filterState||c.estado===this.filterState)&&(!this.filterRisk||c.riesgo===this.filterRisk));}
 select(c:any){this.api.get('/cambios/'+c.id).subscribe({next:v=>this.selected=v,error:e=>this.error=errorMessage(e)});
   this.api.get('/cambios/'+c.id+'/etapas').subscribe({next:v=>this.stages=v,error:e=>this.error=errorMessage(e)});
   this.api.get('/cambios/'+c.id+'/trazabilidad').subscribe({next:v=>this.baseline=v,error:e=>this.error=errorMessage(e)});}
 create(){this.form={proyecto_id:this.api.projectId,codigo:'',solicitante:'Coordinación de obra',descripcion:'',justificacion:'',tipo:'FUNCIONAL',prioridad:'MEDIA',riesgo:'MEDIO',impacto:'',responsable:'Jefe de Proyecto',fecha_comprometida:'2027-01-20',costo_estimado:0,item_id:null,nueva_duracion:null,asignacion_id:null,nueva_cantidad:null};this.editing=true;}
 edit(c:any){this.form={...c};this.editing=true;}
 save(){(this.form.id?this.api.put('/cambios/'+this.form.id,this.form):this.api.post('/cambios',this.form)).subscribe({next:()=>{this.editing=false;this.message='Solicitud guardada';this.load();},error:e=>this.error=errorMessage(e)});}
 approve(){if(!confirm('¿Aprobar y aplicar las modificaciones a planificación y recursos?'))return;
 this.api.post('/cambios/'+this.selected.id+'/aprobar').subscribe({next:()=>{this.message='Cambio aprobado, línea base guardada y planificación actualizada';this.select(this.selected);this.load();},error:e=>this.error=errorMessage(e)});}
 reject(){const reason=prompt('Motivo del rechazo:');if(!reason)return;
 this.api.post('/cambios/'+this.selected.id+'/rechazar',{resultado:reason}).subscribe({next:()=>{this.message='Solicitud rechazada';this.select(this.selected);this.load();},error:e=>this.error=errorMessage(e)});}
 completeStage(s:any){const evidence=prompt('Evidencia de la etapa '+s.nombre+':','Completada y validada por responsable');if(evidence===null)return;
 this.api.put('/cambios/'+this.selected.id+'/etapas/'+s.orden,{completada:true,evidencia:evidence,responsable:'Jefe de Proyecto'}).subscribe({next:()=>{this.select(this.selected);this.message='Etapa completada';},error:e=>this.error=errorMessage(e)});}
 close(){this.api.post('/cambios/'+this.selected.id+'/cerrar',this.closeForm).subscribe({next:()=>{this.message='Cambio cerrado';this.select(this.selected);this.load();},error:e=>this.error=errorMessage(e)});}
}
