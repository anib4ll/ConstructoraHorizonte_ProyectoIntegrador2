import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../services/api.service';
import { errorMessage,fmtMoney,fmtDate } from '../shared';

@Component({selector:'app-dashboards',standalone:true,imports:[CommonModule,FormsModule],template:`
<section class="panel"><div class="section-head"><div><h2>{{kind==='proyecto'?'Dashboard de ejecución del proyecto':'Dashboard de gestión del cambio'}}</h2><p>Datos reales de la API y la misma base de datos que utilizan los módulos operativos.</p></div><button class="outline" (click)="load()">Actualizar datos</button></div>
 <div class="filters" *ngIf="kind==='proyecto'"><label>Etapa<select [(ngModel)]="stage"><option value="">Todas</option><option>1.1</option><option>1.2</option><option>1.3</option><option>1.4</option></select></label><label>Responsable<input [(ngModel)]="person" placeholder="Todos"></label><label>Desde<input type="date" [(ngModel)]="from"></label><label>Hasta<input type="date" [(ngModel)]="to"></label><button class="outline" (click)="load()">Filtrar</button></div>
 <div class="filters" *ngIf="kind==='cambios'"><label>Estado<select [(ngModel)]="state"><option value="">Todos</option><option *ngFor="let s of states">{{s}}</option></select></label><label>Riesgo<select [(ngModel)]="risk"><option value="">Todos</option><option>BAJO</option><option>MEDIO</option><option>ALTO</option><option>CRITICO</option></select></label><label>Responsable<input [(ngModel)]="person" placeholder="Todos"></label><button class="outline" (click)="load()">Filtrar</button></div>
 <p *ngIf="error" class="error">{{error}}</p>
</section>
<ng-container *ngIf="data && kind==='proyecto'">
<section class="stats-row"><div><strong>{{data.avanceFiltrado | number:'1.0-1'}}%</strong><span>Avance de subactividades filtradas</span></div><div><strong>{{data.atrasadas}}</strong><span>Subactividades atrasadas</span></div><div><strong>{{fmtMoney(data.costos.plan)}}</strong><span>Costo planificado total</span></div><div><strong>{{fmtMoney(data.costos.real)}}</strong><span>Costo real total</span></div><div><strong>{{data.costos.alertas?.length||0}}</strong><span>Alertas de recursos</span></div></section>
<section class="panel"><h2>Progreso por subactividad</h2><div class="chart-list"><div class="chart-row" *ngFor="let a of data.actividades"><span>{{a.codigo}}</span><div class="chart-track"><div [style.width.%]="a.progreso"></div></div><strong>{{a.progreso|number:'1.0-0'}}%</strong><small>{{a.nombre}}</small></div></div></section>
<section class="panel"><h2>Hitos del proyecto</h2><div class="table-wrap"><table><thead><tr><th>Código</th><th>Hito</th><th>Fecha</th><th>Estado</th><th>Criterio</th></tr></thead><tbody><tr *ngFor="let h of data.hitos"><td>{{h.codigo}}</td><td>{{h.descripcion}}</td><td>{{fmtDate(h.fecha_comprometida)}}</td><td>{{h.estado}}</td><td>{{h.criterio}}</td></tr></tbody></table></div></section>
</ng-container>
<ng-container *ngIf="data && kind==='cambios'">
<section class="stats-row"><div><strong>{{data.solicitudes.length}}</strong><span>Solicitudes filtradas</span></div><div><strong>{{data.etapasCompletadas}} / {{data.etapasTotales}}</strong><span>Etapas del cambio completadas</span></div><div><strong>{{fmtMoney(data.costoEstimado)}}</strong><span>Costo estimado</span></div><div><strong>{{fmtMoney(data.costoReal)}}</strong><span>Costo real</span></div><div><strong>{{data.atrasadas}}</strong><span>Solicitudes atrasadas</span></div></section>
<section class="cards"><article class="panel"><h2>Solicitudes por estado</h2><div class="metric-line" *ngFor="let entry of entries(data.porEstado)"><span>{{entry[0]}}</span><div class="metric-bar"><div [style.width.%]="percentage(entry[1])"></div></div><strong>{{entry[1]}}</strong></div></article>
<article class="panel"><h2>Solicitudes por riesgo</h2><div class="metric-line" *ngFor="let entry of entries(data.porRiesgo)"><span>{{entry[0]}}</span><div class="metric-bar"><div [style.width.%]="percentage(entry[1])"></div></div><strong>{{entry[1]}}</strong></div></article></section>
<section class="panel"><h2>Resumen de solicitudes</h2><div class="table-wrap"><table><thead><tr><th>Código</th><th>Descripción</th><th>Prioridad</th><th>Riesgo</th><th>Estado</th><th>Fecha comprometida</th></tr></thead><tbody><tr *ngFor="let c of data.solicitudes"><td>{{c.codigo}}</td><td>{{c.descripcion}}</td><td>{{c.prioridad}}</td><td>{{c.riesgo}}</td><td>{{c.estado}}</td><td>{{fmtDate(c.fecha_comprometida)}}</td></tr></tbody></table></div></section>
</ng-container>
<section *ngIf="data" class="panel"><h2>{{kind==='proyecto'?'KPI del proyecto':'KPI de gestión del cambio'}}</h2><div class="kpi-grid"><article *ngFor="let k of filteredKpis()" class="kpi-box"><div class="row-split"><span>{{k.codigo}}</span><span class="status-dot" [class.green]="k.semaforo==='VERDE'" [class.yellow]="k.semaforo==='AMARILLO'" [class.red]="k.semaforo==='ROJO'"></span></div><strong>{{k.valor | number:'1.0-2'}} {{k.unidad}}</strong><p>{{k.nombre}}</p><small>Meta: {{k.sentido==='MENOR'?'≤':'≥'}} {{k.meta}} {{k.unidad}}</small></article></div></section>`})
export class DashboardsComponent implements OnInit{
 @Input() kind='proyecto';
 data:any=null;error='';stage='';state='';risk='';person='';from='';to='';fmtMoney=fmtMoney;fmtDate=fmtDate;
 states=['ABIERTO','EN_EVALUACION','APROBADO','RECHAZADO','EN_IMPLEMENTACION','CERRADO'];
 constructor(private api:ApiService){}
 ngOnInit(){this.load();}
 load(){const query:string[]=['proyectoId='+this.api.projectId];if(this.kind==='proyecto'){
  if(this.stage)query.push('etapa='+encodeURIComponent(this.stage));if(this.person)query.push('responsable='+encodeURIComponent(this.person));
  if(this.from)query.push('desde='+this.from);if(this.to)query.push('hasta='+this.to);
 }else{if(this.state)query.push('estado='+this.state);if(this.risk)query.push('riesgo='+this.risk);if(this.person)query.push('responsable='+encodeURIComponent(this.person));}
 this.api.get('/dashboard/'+this.kind+'?'+query.join('&')).subscribe({next:v=>{this.data=v;this.error='';},error:e=>this.error=errorMessage(e)});}
 filteredKpis(){return (this.data?.kpis||[]).filter((k:any)=>k.ambito===(this.kind==='proyecto'?'PROYECTO':'CAMBIO'));}
 percentage(v:any){return this.data?.solicitudes?.length ? 100*Number(v)/this.data.solicitudes.length : 0;}
 entries(o:any):[string,any][]{return Object.entries(o||{});}
}
