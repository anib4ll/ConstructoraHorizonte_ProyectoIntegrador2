import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../services/api.service';
import { errorMessage,fmtMoney,fmtDate } from '../shared';

@Component({selector:'app-projects',standalone:true,imports:[CommonModule,FormsModule],template:`
<section class="panel">
  <div class="section-head"><div><h2>Administración de proyectos</h2><p>Alta, edición, consulta y cierre de proyectos.</p></div><button *ngIf="admin" class="primary" (click)="newProject()">+ Nuevo proyecto</button></div>
  <p *ngIf="error" class="error">{{error}}</p><p *ngIf="success" class="success">{{success}}</p>
  <div class="cards">
    <article class="project-card" *ngFor="let p of projects">
      <div class="row-split"><span class="tag">{{p.codigo}}</span><span class="tag pale">{{p.estado}}</span></div>
      <h3>{{p.nombre}}</h3><p>{{p.descripcion}}</p>
      <dl><dt>Responsable</dt><dd>{{p.responsable}}</dd><dt>Plazo</dt><dd>{{fmtDate(p.inicio)}} – {{fmtDate(p.fin)}}</dd>
      <dt>Presupuesto</dt><dd>{{fmtMoney(p.presupuesto)}}</dd><dt>Avance</dt><dd>{{p.avance | number:'1.0-1'}}%</dd></dl>
      <div class="progress-track"><div [style.width.%]="p.avance"></div></div>
      <button *ngIf="admin" class="outline" (click)="edit(p)">Editar proyecto</button>
    </article>
  </div>
</section>
<section *ngIf="formVisible&&admin" class="panel editor">
  <h2>{{editing?'Editar proyecto':'Crear proyecto'}}</h2>
  <form (ngSubmit)="save()" class="form-grid">
    <label>Código<input [disabled]="editing" name="codigo" [(ngModel)]="form.codigo" required></label>
    <label>Nombre<input name="nombre" [(ngModel)]="form.nombre" required></label>
    <label>Responsable<input name="responsable" [(ngModel)]="form.responsable"></label>
    <label>Presupuesto (CLP)<input name="presupuesto" type="number" min="0" [(ngModel)]="form.presupuesto"></label>
    <label>Inicio<input name="inicio" type="date" [(ngModel)]="form.inicio" required></label>
    <label>Término<input name="fin" type="date" [(ngModel)]="form.fin" required></label>
    <label *ngIf="editing">Estado<select name="estado" [(ngModel)]="form.estado"><option *ngFor="let e of states">{{e}}</option></select></label>
    <label class="span-2">Descripción<textarea name="descripcion" [(ngModel)]="form.descripcion" rows="3"></textarea></label>
    <div class="form-actions span-2"><button type="submit" class="primary">Guardar cambios</button><button type="button" class="outline" (click)="formVisible=false">Cancelar</button></div>
  </form>
</section>`})
export class ProjectsComponent implements OnInit {
  @Input() admin=false;
  @Output() projectCreated=new EventEmitter<number>();
  projects:any[]=[]; form:any={}; editing=false;formVisible=false; error='';success='';
  states=['PLANIFICADO','EN_EJECUCION','SUSPENDIDO','TERMINADO'];
  fmtMoney=fmtMoney;fmtDate=fmtDate;
  constructor(private api:ApiService){}
  ngOnInit(){this.reload();}
  reload(){this.api.get('/proyectos').subscribe({next:(v:any)=>this.projects=v,error:(e:any)=>this.error=errorMessage(e)});}
  newProject(){this.editing=false;this.form={codigo:'',nombre:'',descripcion:'',responsable:'',inicio:'2026-11-05',fin:'2027-07-02',presupuesto:0};this.formVisible=true;}
  edit(p:any){this.editing=true;this.form={...p};this.formVisible=true;}
  save(){this.error='';const request=this.editing?this.api.put('/proyectos/'+this.form.id,this.form):this.api.post('/proyectos',this.form);
    request.subscribe({next:(saved:any)=>{this.formVisible=false;this.success='Proyecto guardado en PostgreSQL';this.reload();if(!this.editing)this.projectCreated.emit(saved.id);},error:(e:any)=>this.error=errorMessage(e)});}
}
