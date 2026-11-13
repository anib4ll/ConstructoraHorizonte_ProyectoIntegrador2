import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../services/api.service';
import { errorMessage } from '../shared';

@Component({selector:'app-kpis',standalone:true,imports:[CommonModule,FormsModule],template:`
<section class="panel"><div class="section-head"><div><h2>Indicadores de seguimiento</h2><p>Ocho KPI del proyecto y doce de gestión del cambio, más indicadores configurables.</p></div><div class="toolbar"><button class="outline" (click)="refresh()">Recalcular</button><button *ngIf="admin" class="primary" (click)="create()">+ KPI</button></div></div>
 <p *ngIf="error" class="error">{{error}}</p><p *ngIf="message" class="success">{{message}}</p>
 <div class="filters"><label>Ámbito<select [(ngModel)]="filter"><option value="">Todos</option><option>PROYECTO</option><option>CAMBIO</option></select></label></div>
 <div class="table-wrap"><table><thead><tr><th>Código</th><th>Indicador</th><th>Fórmula</th><th>Valor</th><th>Meta</th><th>Estado</th><th>Acción</th></tr></thead>
 <tbody><tr *ngFor="let k of filtered()"><td>{{k.codigo}}</td><td><strong>{{k.nombre}}</strong><small class="block subtle">{{k.ambito}} · {{k.unidad}}</small></td><td><code>{{k.formula}}</code></td><td><strong>{{k.valor | number:'1.0-2'}}</strong></td><td>{{k.sentido==='MENOR'?'≤':'≥'}} {{k.meta}}</td><td><span class="status-dot" [class.green]="k.semaforo==='VERDE'" [class.yellow]="k.semaforo==='AMARILLO'" [class.red]="k.semaforo==='ROJO'"></span>{{k.semaforo}}</td>
 <td><div class="toolbar"><button class="small outline" (click)="showHistory(k)">Historial</button><button *ngIf="admin" class="small outline" (click)="edit(k)">Editar</button></div></td></tr></tbody></table></div>
</section>
<section *ngIf="historyItem" class="panel"><div class="section-head"><h2>Historial: {{historyItem.codigo}}</h2><button class="outline" (click)="historyItem=null">Cerrar</button></div>
 <div class="table-wrap"><table><thead><tr><th>Fecha</th><th>Valor registrado</th><th>Meta</th><th>Semáforo</th></tr></thead><tbody><tr *ngFor="let m of history"><td>{{m.fecha}}</td><td>{{m.valor}}</td><td>{{m.meta}}</td><td>{{m.semaforo}}</td></tr></tbody></table></div>
</section>
<section *ngIf="editing&&admin" class="panel editor"><h2>{{form.id?'Editar KPI':'Nuevo KPI'}}</h2>
 <p class="subtle">La fórmula acepta variables disponibles y operaciones +, -, *, / y paréntesis. No se ejecuta código externo.</p>
 <form (ngSubmit)="save()" class="form-grid">
  <label *ngIf="!form.id">Código<input name="codigo" [(ngModel)]="form.codigo" required></label>
  <label>Nombre<input name="nombre" [(ngModel)]="form.nombre" required></label>
  <label>Ámbito<select name="ambito" [(ngModel)]="form.ambito" [disabled]="!!form.id"><option>PROYECTO</option><option>CAMBIO</option></select></label>
  <label>Sentido<select name="sentido" [(ngModel)]="form.sentido"><option>MAYOR</option><option>MENOR</option><option>RANGO</option></select></label>
  <label>Meta<input type="number" name="meta" [(ngModel)]="form.meta" required></label>
  <label>Umbral de alerta<input type="number" name="umbral" [(ngModel)]="form.umbral" required></label>
  <label>Unidad<input name="unidad" [(ngModel)]="form.unidad"></label>
  <label>Periodicidad<input name="periodicidad" [(ngModel)]="form.periodicidad"></label>
  <label class="span-2">Fórmula<input name="formula" [(ngModel)]="form.formula" required></label>
  <label *ngIf="form.id">Activo <input type="checkbox" name="activo" [(ngModel)]="form.activo"></label>
  <label class="span-2">Objetivo<textarea name="objetivo" [(ngModel)]="form.objetivo"></textarea></label>
  <div class="form-actions span-2"><button class="primary" type="submit">Guardar indicador</button><button class="outline" type="button" (click)="editing=false">Cancelar</button></div>
 </form>
 <details><summary>Ver variables permitidas</summary><div class="chips"><code class="tag" *ngFor="let v of variableNames">{{v}}</code></div></details>
</section>`})
export class KpisComponent implements OnInit{
 @Input() admin=false;
 indicators:any[]=[];variables:any={};variableNames:string[]=[];filter='';form:any={};editing=false;error='';message='';
 historyItem:any=null;history:any[]=[];
 constructor(private api:ApiService){}
 ngOnInit(){this.load();}
 load(){this.api.get(`/kpis?proyectoId=${this.api.projectId}`).subscribe({next:v=>this.indicators=v,error:e=>this.error=errorMessage(e)});
  this.api.get(`/kpis/variables?proyectoId=${this.api.projectId}`).subscribe({next:v=>{this.variables=v;this.variableNames=Object.keys(v);},error:e=>this.error=errorMessage(e)});}
 filtered(){return this.indicators.filter(k=>!this.filter||k.ambito===this.filter);}
 refresh(){this.api.post(`/kpis/evaluar?proyectoId=${this.api.projectId}`).subscribe({next:()=>{this.message='KPI recalculados';this.load();},error:e=>this.error=errorMessage(e)});}
 create(){this.form={proyecto_id:this.api.projectId,codigo:'',nombre:'',objetivo:'',ambito:'PROYECTO',formula:'progreso',sentido:'MAYOR',meta:90,umbral:75,unidad:'%',periodicidad:'Semanal'};this.editing=true;}
 edit(k:any){this.form={...k};this.editing=true;}
 save(){(this.form.id?this.api.put('/kpis/'+this.form.id,this.form):this.api.post('/kpis',this.form)).subscribe({next:()=>{this.editing=false;this.message='Configuración guardada';this.load();},error:e=>this.error=errorMessage(e)});}
 showHistory(k:any){this.historyItem=k;this.api.get('/kpis/'+k.id+'/historial').subscribe({next:v=>this.history=v,error:e=>this.error=errorMessage(e)});}
}
