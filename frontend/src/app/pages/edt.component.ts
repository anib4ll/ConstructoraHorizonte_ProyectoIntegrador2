import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../services/api.service';
import { errorMessage,fmtDate } from '../shared';

@Component({selector:'app-edt',standalone:true,imports:[CommonModule,FormsModule],template:`
<section class="panel">
 <div class="section-head"><div><h2>Estructura de Desglose del Trabajo</h2><p>Proyecto → Etapa → Actividad → Subactividad. Los valores de padres se consolidan desde las hojas.</p></div><button class="outline" (click)="load()">Actualizar</button></div>
 <div *ngIf="admin" class="toolbar"><label>Importar Excel EDT (.xlsx) <input type="file" accept=".xlsx" (change)="importExcel($event)" [disabled]="importing"></label></div>
 <div class="stats-row"><div><strong>{{count('ETAPA')}}</strong><span>Etapas</span></div><div><strong>{{count('ACTIVIDAD')}}</strong><span>Actividades</span></div><div><strong>{{count('SUBACTIVIDAD')}}</strong><span>Subactividades</span></div></div>
 <p *ngIf="error" class="error">{{error}}</p><p *ngIf="success" class="success">{{success}}</p>
 <div class="table-wrap"><table><thead><tr><th>Código / componente</th><th>Responsable</th><th>Inicio</th><th>Término</th><th>Avance</th><th>Acciones</th></tr></thead>
 <tbody><tr *ngFor="let x of visibleItems()" [class.row-stage]="x.tipo==='ETAPA'" [class.row-activity]="x.tipo==='ACTIVIDAD'">
   <td><div [style.padding-left.px]="depth(x)*22" class="edt-name"><button *ngIf="x.tipo!=='SUBACTIVIDAD'" class="toggle" (click)="toggle(x)">{{collapsed.has(x.id)?'▸':'▾'}}</button><span *ngIf="x.tipo==='SUBACTIVIDAD'" class="leaf-mark">•</span><div><strong>{{x.codigo}}</strong> {{x.nombre}}<small>{{x.tipo}}</small></div></div></td>
   <td>{{x.responsable}}</td><td>{{fmtDate(x.inicio)}}</td><td>{{fmtDate(x.fin)}}</td><td><div class="inline-progress"><span [style.width.%]="x.progreso"></span></div>{{x.progreso|number:'1.0-0'}}%</td>
   <td class="actions"><button *ngIf="admin" class="small outline" (click)="edit(x)">Editar</button><button *ngIf="admin&&x.tipo!=='SUBACTIVIDAD'" class="small outline" (click)="createChild(x)">+ Hijo</button><button *ngIf="!admin&&x.tipo==='SUBACTIVIDAD'" class="small outline" (click)="edit(x)">Avance</button><button *ngIf="admin" class="small danger-outline" (click)="remove(x)">×</button></td>
 </tr></tbody></table></div>
 <div *ngIf="admin" class="toolbar"><button class="outline" (click)="createStage()">+ Nueva etapa</button></div>
</section>
<section class="panel editor" *ngIf="editing">
 <h2>{{form.id?'Editar componente':'Agregar componente'}}</h2>
 <form (ngSubmit)="save()" class="form-grid">
   <label>Código<input name="codigo" [(ngModel)]="form.codigo" [disabled]="!!form.id || !admin" required></label>
   <label>Tipo<input name="tipo" [(ngModel)]="form.tipo" disabled></label>
   <label class="span-2">Nombre<input name="nombre" [(ngModel)]="form.nombre" [disabled]="!admin" required></label>
   <label *ngIf="admin">Responsable<input name="responsable" [(ngModel)]="form.responsable"></label>
   <label *ngIf="admin&&form.tipo==='SUBACTIVIDAD'">Inicio base<input name="inicio_base" type="date" [(ngModel)]="form.inicio_base"></label>
   <label *ngIf="admin&&form.tipo==='SUBACTIVIDAD'">Duración (días hábiles)<input name="duracion" type="number" min="1" [(ngModel)]="form.duracion"></label>
   <label *ngIf="form.tipo==='SUBACTIVIDAD'">Avance %<input name="progreso" type="number" min="0" max="100" [(ngModel)]="form.progreso"></label>
   <label class="span-2" *ngIf="admin">Descripción<textarea name="descripcion" rows="2" [(ngModel)]="form.descripcion"></textarea></label>
   <div class="form-actions span-2"><button class="primary" type="submit">Guardar</button><button type="button" class="outline" (click)="editing=false">Cancelar</button></div>
 </form>
</section>`})
export class EdtComponent implements OnInit {
 @Input() admin=false;
 importing=false;items:any[]=[];error='';success='';collapsed=new Set<number>();form:any={};editing=false;
 fmtDate=fmtDate;
 constructor(private api:ApiService){}
 ngOnInit(){this.load();}
 load(){this.api.get(`/edt?proyectoId=${this.api.projectId}`).subscribe({next:(v:any)=>this.items=v,error:(e:any)=>this.error=errorMessage(e)});}
 importExcel(event:Event){const input=event.target as HTMLInputElement;const file=input.files?.[0];if(!file)return;this.importing=true;this.error='';this.success='';this.api.uploadExcel('/proyectos/'+this.api.projectId+'/edt/importar-excel',file).subscribe({next:(v:any)=>{this.importing=false;this.success=v.importados+' registros importados';this.load();input.value='';},error:(e:any)=>{this.importing=false;this.error=errorMessage(e);input.value='';}});}
 count(type:string){return this.items.filter(x=>x.tipo===type).length;}
 depth(x:any){return x.tipo==='ETAPA'?0:x.tipo==='ACTIVIDAD'?1:2;}
 toggle(x:any){if(this.collapsed.has(x.id))this.collapsed.delete(x.id);else this.collapsed.add(x.id);}
 visibleItems(){return this.items.filter(x=>{let parent=x.parent_id;while(parent){if(this.collapsed.has(parent))return false; parent=this.items.find(n=>n.id===parent)?.parent_id;}return true;});}
 edit(x:any){this.form={...x};this.editing=true;this.error='';}
 createStage(){this.form={proyecto_id:this.api.projectId,parent_id:null,codigo:'',nombre:'',descripcion:'',responsable:'',tipo:'ETAPA',duracion:5};this.editing=true;}
 createChild(p:any){this.form={proyecto_id:this.api.projectId,parent_id:p.id,codigo:p.codigo+'.',nombre:'',descripcion:'',responsable:'',tipo:p.tipo==='ETAPA'?'ACTIVIDAD':'SUBACTIVIDAD',inicio_base:p.inicio,duracion:5,progreso:0};this.editing=true;}
 save(){this.error='';if(!this.admin){this.api.patch('/edt/'+this.form.id+'/avance',{progreso:this.form.progreso}).subscribe({next:()=>{this.editing=false;this.success='Avance registrado';this.load();},error:e=>this.error=errorMessage(e)});return;}
  const data={...this.form};
  if(data.tipo!=='SUBACTIVIDAD'){delete data.duracion;delete data.inicio_base;}
  const req=data.id?this.api.put('/edt/'+data.id,data):this.api.post('/edt',data);
  req.subscribe({next:()=>{this.editing=false;this.success='EDT actualizada y consolidada';this.load();},error:e=>this.error=errorMessage(e)});
 }
 remove(x:any){if(!confirm('¿Eliminar '+x.codigo+'? No se permitirá si tiene hijos o relaciones.'))return;
  this.api.delete('/edt/'+x.id).subscribe({next:()=>this.load(),error:e=>this.error=errorMessage(e)});}
}
