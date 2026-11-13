import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../services/api.service';
import { errorMessage, fmtMoney } from '../shared';

@Component({selector:'app-resources',standalone:true,imports:[CommonModule,FormsModule],template:`
<section class="panel">
 <div class="section-head"><div><h2>Recursos y presupuesto</h2><p>El costo se calcula a partir de las asignaciones a subactividades, sin duplicar el total de la EDT.</p></div><button class="outline" (click)="load()">Actualizar cálculos</button></div>
 <p *ngIf="error" class="error">{{error}}</p><p *ngIf="message" class="success">{{message}}</p>
 <div class="stats-row"><div><strong>{{fmtMoney(costs.plan)}}</strong><span>Costo planificado</span></div><div><strong>{{fmtMoney(costs.real)}}</strong><span>Costo real registrado</span></div><div><strong>{{resources.length}}</strong><span>Recursos disponibles</span></div><div><strong>{{costs.alertas?.length||0}}</strong><span>Alertas de sobreasignación</span></div></div>
 <div *ngIf="costs.alertas?.length" class="notice warning"><strong>Alertas de capacidad semanal</strong><p *ngFor="let a of costs.alertas">Profesional #{{a.recursoId}} — semana {{a.semana}}: {{a.horasAsignadas}} h asignadas / {{a.horasDisponibles}} h disponibles.</p></div>
</section>
<section class="panel" *ngIf="costs.porItem?.length"><h2>Costos consolidados por etapa y actividad</h2>
 <div class="table-wrap"><table><thead><tr><th>Código EDT</th><th>Componente</th><th>Costo planificado</th><th>Costo real</th></tr></thead><tbody>
 <tr *ngFor="let x of costs.porItem" [hidden]="x.tipo==='SUBACTIVIDAD'"><td>{{x.codigo}}</td><td>{{x.nombre}}</td><td>{{fmtMoney(x.costo_plan)}}</td><td>{{fmtMoney(x.costo_real)}}</td></tr>
 </tbody></table></div></section>
<section class="panel">
 <div class="section-head"><h2>Catálogo de recursos</h2><button *ngIf="admin" class="outline" (click)="resourceForm=true">+ Recurso</button></div>
 <div class="table-wrap"><table><thead><tr><th>Código</th><th>Nombre</th><th>Tipo</th><th>Tarifa/hora</th><th>Valor unidad</th><th>Disponibilidad</th><th></th></tr></thead>
 <tbody><tr *ngFor="let r of resources"><td>{{r.codigo}}</td><td>{{r.nombre}}</td><td>{{r.tipo}}</td><td>{{fmtMoney(r.tarifa_hora)}}</td><td>{{fmtMoney(r.costo_unidad)}}</td><td>{{r.tipo==='PROFESIONAL'?r.horas_disponibles+' h/semana':r.cantidad_disponible+' unidades'}}</td><td><button *ngIf="admin" class="small outline" (click)="editResource(r)">Editar</button><button *ngIf="admin" class="small danger-outline" (click)="removeResource(r)">Eliminar</button></td></tr></tbody></table></div>
</section>
<section *ngIf="resourceForm&&admin" class="panel editor"><h2>{{newResource.id?'Editar recurso':'Crear recurso'}}</h2>
 <form class="form-grid" (ngSubmit)="saveResource()">
  <label>Código<input name="codigo" [(ngModel)]="newResource.codigo" [disabled]="!!newResource.id" required /></label>
  <label>Nombre<input name="nombre" [(ngModel)]="newResource.nombre" required /></label>
  <label>Tipo<select name="tipo" [(ngModel)]="newResource.tipo" [disabled]="!!newResource.id"><option *ngFor="let t of types">{{t}}</option></select></label>
  <label>Especialidad<input name="especialidad" [(ngModel)]="newResource.especialidad" /></label>
  <label>Tarifa por hora<input type="number" name="tarifa_hora" min="0" [(ngModel)]="newResource.tarifa_hora" /></label>
  <label>Costo por unidad<input type="number" name="costo_unidad" min="0" [(ngModel)]="newResource.costo_unidad" /></label>
  <label>Horas disponibles/semana<input type="number" name="horas_disponibles" min="0" [(ngModel)]="newResource.horas_disponibles" /></label>
  <label>Unidades disponibles<input type="number" name="cantidad_disponible" min="0" [(ngModel)]="newResource.cantidad_disponible" /></label>
  <div class="form-actions span-2"><button class="primary">Guardar</button><button type="button" class="outline" (click)="resourceForm=false">Cancelar</button></div>
 </form>
</section>
<section class="panel">
 <div class="section-head"><div><h2>Asignaciones a subactividades</h2><p>Profesionales: horas semanales × semanas × tarifa × cantidad. Materiales y equipos: cantidad × valor unitario.</p></div><button *ngIf="admin" class="outline" (click)="createAssignment()">+ Asignar</button></div>
 <div class="table-wrap"><table><thead><tr><th>EDT</th><th>Recurso</th><th>Cantidad</th><th>Horas/semana</th><th>Semanas</th><th>Plan</th><th>Real</th><th></th></tr></thead>
 <tbody><tr *ngFor="let a of assignments"><td>{{a.codigo_actividad}}</td><td>{{a.recurso}}</td><td>{{a.cantidad}}</td><td>{{a.horas_semana}}</td><td>{{a.semanas}}</td><td>{{fmtMoney(a.costo_plan)}}</td><td>{{fmtMoney(a.costo_real)}}</td><td><button *ngIf="admin" class="small outline" (click)="editAssignment(a)">Editar</button></td></tr></tbody></table></div>
</section>
<section *ngIf="assignmentForm&&admin" class="panel editor"><h2>{{newAssignment.id?'Modificar asignación':'Asignar recurso'}}</h2>
 <form class="form-grid" (ngSubmit)="saveAssignment()">
  <label *ngIf="!newAssignment.id">Subactividad<select name="item_id" [(ngModel)]="newAssignment.item_id"><option *ngFor="let x of leaves" [ngValue]="x.id">{{x.codigo}} {{x.nombre}}</option></select></label>
  <label *ngIf="!newAssignment.id">Recurso<select name="recurso_id" [(ngModel)]="newAssignment.recurso_id"><option *ngFor="let x of resources" [ngValue]="x.id">{{x.codigo}} {{x.nombre}}</option></select></label>
  <label>Cantidad planificada<input type="number" name="cantidad" min="0" [(ngModel)]="newAssignment.cantidad" /></label>
  <label>Cantidad real<input type="number" name="cantidad_real" min="0" [(ngModel)]="newAssignment.cantidad_real" /></label>
  <label>Horas semanales planificadas<input type="number" name="horas_semana" min="0" [(ngModel)]="newAssignment.horas_semana" /></label>
  <label>Horas semanales reales<input type="number" name="horas_semana_real" min="0" [(ngModel)]="newAssignment.horas_semana_real" /></label>
  <label>Semanas planificadas<input type="number" name="semanas" min="0" [(ngModel)]="newAssignment.semanas" /></label>
  <label>Semanas reales<input type="number" name="semanas_reales" min="0" [(ngModel)]="newAssignment.semanas_reales" /></label>
  <div class="form-actions span-2"><button class="primary">Guardar y recalcular</button><button type="button" class="outline" (click)="assignmentForm=false">Cancelar</button></div>
 </form>
</section>`})
export class ResourcesComponent implements OnInit {
 @Input() admin=false;
 resources:any[]=[];assignments:any[]=[];leaves:any[]=[];costs:any={plan:0,real:0,alertas:[]};
 resourceForm=false;assignmentForm=false;newResource:any={};newAssignment:any={};error='';message='';
 types=['PROFESIONAL','TIPO_PROFESIONAL','LICENCIA','MATERIAL','EQUIPAMIENTO','OTRO'];fmtMoney=fmtMoney;
 constructor(private api:ApiService){}
 ngOnInit(){this.load();}
 load(){this.api.get(`/recursos?proyectoId=${this.api.projectId}`).subscribe({next:(v:any)=>this.resources=v,error:e=>this.error=errorMessage(e)});
  this.api.get(`/asignaciones?proyectoId=${this.api.projectId}`).subscribe({next:(v:any)=>this.assignments=v,error:e=>this.error=errorMessage(e)});
  this.api.get(`/costos?proyectoId=${this.api.projectId}`).subscribe({next:(v:any)=>this.costs=v,error:e=>this.error=errorMessage(e)});
  this.api.get(`/edt?proyectoId=${this.api.projectId}`).subscribe({next:(v:any)=>this.leaves=v.filter((i:any)=>i.tipo==='SUBACTIVIDAD'),error:e=>this.error=errorMessage(e)});
 }
 removeResource(r:any){if(!confirm('¿Eliminar recurso '+r.codigo+'? Solo es posible sin asignaciones.'))return;
 this.api.delete('/recursos/'+r.id).subscribe({next:()=>{this.message='Recurso eliminado';this.load();},error:e=>this.error=errorMessage(e)});}
 editResource(r:any){this.newResource={...r};this.resourceForm=true;}
 saveResource(){const x={proyecto_id:this.api.projectId,...this.newResource};(x.id?this.api.put('/recursos/'+x.id,x):this.api.post('/recursos',x)).subscribe({next:()=>{this.resourceForm=false;this.message='Recurso guardado';this.load();},error:e=>this.error=errorMessage(e)});}
 createAssignment(){this.newAssignment={proyecto_id:this.api.projectId,item_id:this.leaves[0]?.id,recurso_id:this.resources[0]?.id,cantidad:1,horas_semana:20,semanas:2};this.assignmentForm=true;}
 editAssignment(a:any){this.newAssignment={...a};this.assignmentForm=true;}
 saveAssignment(){const x={proyecto_id:this.api.projectId,...this.newAssignment};(x.id?this.api.put('/asignaciones/'+x.id,x):this.api.post('/asignaciones',x)).subscribe({next:()=>{this.assignmentForm=false;this.message='Asignación actualizada';this.load();},error:e=>this.error=errorMessage(e)});}
}
