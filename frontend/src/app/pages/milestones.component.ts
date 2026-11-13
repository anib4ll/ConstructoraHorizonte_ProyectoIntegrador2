import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../services/api.service';
import { errorMessage,fmtDate } from '../shared';

@Component({selector:'app-milestones',standalone:true,imports:[CommonModule,FormsModule],template:`
<section class="panel">
 <div class="section-head"><div><h2>Hitos y criterios de aceptación</h2><p>Se representan con duración cero en la Carta Gantt. Cada hito tiene fecha comprometida, estado y evidencia.</p></div><button *ngIf="admin" class="outline" (click)="create()">+ Nuevo hito</button></div>
 <p *ngIf="error" class="error">{{error}}</p><p *ngIf="message" class="success">{{message}}</p>
 <div class="table-wrap"><table><thead><tr><th>Código</th><th>Descripción</th><th>Fecha compromiso</th><th>Fecha real</th><th>Estado</th><th>Evidencia / criterio</th><th></th></tr></thead><tbody>
 <tr *ngFor="let h of milestones"><td>{{h.codigo}}</td><td>{{h.descripcion}}<small class="block subtle">{{h.responsable}}</small></td><td>{{fmtDate(h.fecha_comprometida)}}</td><td>{{fmtDate(h.fecha_real)}}</td><td><span class="tag">{{h.estado}}</span></td><td><strong>{{h.criterio}}</strong><small class="block">{{h.evidencia}}</small></td><td><button *ngIf="admin" class="small outline" (click)="edit(h)">Editar</button><button *ngIf="admin" class="small danger-outline" (click)="remove(h)">Eliminar</button><button *ngIf="!admin" class="small outline" (click)="edit(h)">Evidencia</button></td></tr>
 </tbody></table></div>
</section>
<section *ngIf="editing" class="panel editor"><h2>{{form.id?'Actualizar hito':'Crear hito'}}</h2>
 <form (ngSubmit)="save()" class="form-grid">
  <label *ngIf="!form.id">Código<input name="codigo" [(ngModel)]="form.codigo" required /></label>
  <label>Descripción<input name="descripcion" [(ngModel)]="form.descripcion" [disabled]="!admin" required /></label>
  <label *ngIf="admin">Responsable<input name="responsable" [(ngModel)]="form.responsable" /></label>
  <label *ngIf="admin">Fecha comprometida<input type="date" name="fecha_comprometida" [(ngModel)]="form.fecha_comprometida" /></label>
  <label *ngIf="admin">Fecha real<input type="date" name="fecha_real" [(ngModel)]="form.fecha_real" /></label>
  <label *ngIf="admin">Estado<select name="estado" [(ngModel)]="form.estado"><option *ngFor="let s of states">{{s}}</option></select></label>
  <label class="span-2" *ngIf="admin">Criterio de aceptación<textarea name="criterio" [(ngModel)]="form.criterio" rows="2"></textarea></label>
  <label class="span-2">Evidencia<textarea name="evidencia" [(ngModel)]="form.evidencia" rows="2"></textarea></label>
  <label class="span-2" *ngIf="admin">Actividad asociada<select multiple name="actividad_ids" [(ngModel)]="form.actividad_ids" size="5"><option *ngFor="let a of activities" [ngValue]="a.id">{{a.codigo}} {{a.nombre}}</option></select></label>
  <div class="form-actions span-2"><button type="submit" class="primary">Guardar</button><button type="button" class="outline" (click)="editing=false">Cancelar</button></div>
 </form>
</section>`})
export class MilestonesComponent implements OnInit{
 @Input() admin=false;
 milestones:any[]=[];activities:any[]=[];form:any={};editing=false;error='';message='';fmtDate=fmtDate;
 states=['PENDIENTE','EN_CURSO','CUMPLIDO','ATRASADO','RECHAZADO'];
 constructor(private api:ApiService){}
 ngOnInit(){this.load();}
 load(){this.api.get(`/hitos?proyectoId=${this.api.projectId}`).subscribe({next:v=>this.milestones=v,error:e=>this.error=errorMessage(e)});
  this.api.get(`/edt?proyectoId=${this.api.projectId}`).subscribe({next:(v:any)=>this.activities=v.filter((x:any)=>x.tipo==='SUBACTIVIDAD'),error:e=>this.error=errorMessage(e)});}
 create(){this.form={proyecto_id:this.api.projectId,codigo:'',descripcion:'',responsable:'Jefe de Proyecto',fecha_comprometida:'2027-05-15',estado:'PENDIENTE',criterio:'',evidencia:'',actividad_ids:[]};this.editing=true;}
 edit(h:any){this.form={...h,fecha_real:h.fecha_real||'',actividad_ids:h.actividad_ids||[]};this.editing=true;}
 remove(h:any){if(!confirm('¿Eliminar el hito '+h.codigo+'?'))return;
 this.api.delete('/hitos/'+h.id).subscribe({next:()=>{this.message='Hito eliminado';this.load();},error:e=>this.error=errorMessage(e)});}
 save(){const req=!this.admin?this.api.patch('/hitos/'+this.form.id+'/evidencia',{evidencia:this.form.evidencia}):this.form.id?this.api.put('/hitos/'+this.form.id,this.form):this.api.post('/hitos',this.form);
  req.subscribe({next:()=>{this.editing=false;this.message='Hito actualizado';this.load();},error:e=>this.error=errorMessage(e)});}
}
