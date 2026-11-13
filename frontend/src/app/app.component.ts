import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from './services/api.service';
import { errorMessage } from './shared';
import { ProjectsComponent } from './pages/projects.component';
import { EdtComponent } from './pages/edt.component';
import { PlanningComponent } from './pages/planning.component';
import { ResourcesComponent } from './pages/resources.component';
import { MilestonesComponent } from './pages/milestones.component';
import { KpisComponent } from './pages/kpis.component';
import { ChangesComponent } from './pages/changes.component';
import { DashboardsComponent } from './pages/dashboards.component';

@Component({
  selector: 'app-root', standalone: true,
  imports: [CommonModule, FormsModule, ProjectsComponent, EdtComponent, PlanningComponent,
    ResourcesComponent, MilestonesComponent, KpisComponent, ChangesComponent, DashboardsComponent],
  template: `
  <ng-container *ngIf="!logged; else application">
    <section class="login-wrap">
      <div class="login-box">
        <div class="brand-mark">CH</div>
        <h1>Constructora Horizonte</h1>
        <p>Sistema integrado de planificación y gestión del cambio</p>
        <form (ngSubmit)="login()">
          <label>Correo de usuario</label><input type="email" name="email" [(ngModel)]="email" required />
          <label>Contraseña</label><input type="password" name="password" [(ngModel)]="password" required />
          <button class="primary full" type="submit">Ingresar</button>
        </form>
        <p *ngIf="error" class="error">{{error}}</p>
        <small>Datos de demostración en README.md. Uso local académico.</small>
      </div>
    </section>
  </ng-container>
  <ng-template #application>
    <div class="shell">
      <aside class="sidebar">
        <div class="brand"><div class="brand-mark">CH</div><span>Constructora<br><strong>Horizonte</strong></span></div>
        <div class="nav-group">PROYECTO INTEGRADOR 2</div>
        <button *ngFor="let item of menu" (click)="screen=item.key" [class.active]="screen===item.key"><span>{{item.icon}}</span>{{item.label}}</button>
        <div class="sidebar-bottom"><small>{{email}}<br>{{role}}</small><button (click)="logout()">Salir de la sesión</button></div>
      </aside>
      <main class="workspace">
        <header class="workspace-head"><div><p class="eyebrow">GESTIÓN DE PROYECTOS DE SOFTWARE</p><h1>{{title()}}</h1></div><div class="toolbar"><label class="project-picker">Proyecto <select [(ngModel)]="activeProjectId" (ngModelChange)="chooseProject($event)"><option *ngFor="let p of projects" [ngValue]="p.id">{{p.codigo}} — {{p.nombre}}</option></select></label><div class="user-chip">{{role==='ROLE_ADMIN'?'Administrador':'Usuario del proyecto'}}</div></div></header>
        <app-projects *ngIf="screen==='projects'" [admin]="admin" (projectCreated)="onProjectCreated($event)"></app-projects>
        <app-edt *ngIf="screen==='edt'" [admin]="admin"></app-edt>
        <app-planning *ngIf="screen==='planning'" [admin]="admin"></app-planning>
        <app-resources *ngIf="screen==='resources'" [admin]="admin"></app-resources>
        <app-milestones *ngIf="screen==='milestones'" [admin]="admin"></app-milestones>
        <app-kpis *ngIf="screen==='kpis'" [admin]="admin"></app-kpis>
        <app-changes *ngIf="screen==='changes'" [admin]="admin"></app-changes>
        <app-dashboards *ngIf="screen==='dashboard-project'" kind="proyecto"></app-dashboards>
        <app-dashboards *ngIf="screen==='dashboard-changes'" kind="cambios"></app-dashboards>
        <section *ngIf="screen==='demo'" class="panel">
          <h2>Demostración integrada requerida por la rúbrica</h2>
          <p>Realiza estas tres modificaciones en secuencia. Cada vista consulta la misma base de datos PostgreSQL.</p>
          <div class="demo-steps">
            <article><strong>1. Cambiar una duración</strong><p>En EDT o Gantt edita la duración de una subactividad. Comprueba el recálculo de sucesoras mediante FS/SS/FF/SF.</p><button class="primary" (click)="screen='planning'">Abrir planificación</button></article>
            <article><strong>2. Modificar un recurso</strong><p>Edita las horas o cantidad asignada. Los costos y las alertas de capacidad se recalculan.</p><button class="primary" (click)="screen='resources'">Abrir recursos</button></article>
            <article><strong>3. Aprobar una solicitud</strong><p>Aprueba CAM-03: aplica nueva duración y cantidad de recurso, registra la línea base y se refleja en los tableros.</p><button class="primary" (click)="screen='changes'">Abrir cambios</button></article>
          </div>
          <p class="subtle">Luego revisa ambos dashboards y el historial KPI para comparar las cifras.</p>
        </section>
      </main>
    </div>
  </ng-template>`
})
export class AppComponent {
  email='admin@horizonte.cl'; password=''; role=''; logged=false; admin=false; error=''; screen='dashboard-project';
  activeProjectId=1;projects:any[]=[];
  menu=[
    {key:'dashboard-project',label:'Dashboard del proyecto',icon:'◫'},
    {key:'projects',label:'Proyectos',icon:'▣'},
    {key:'edt',label:'EDT / actividades',icon:'☷'},
    {key:'planning',label:'Carta Gantt',icon:'▤'},
    {key:'resources',label:'Recursos y costos',icon:'▥'},
    {key:'milestones',label:'Hitos',icon:'◇'},
    {key:'kpis',label:'Indicadores KPI',icon:'◉'},
    {key:'changes',label:'Solicitudes de cambio',icon:'⇄'},
    {key:'dashboard-changes',label:'Dashboard de cambios',icon:'▦'},
    {key:'demo',label:'Demostración integrada',icon:'▶'}
  ];
  constructor(private api:ApiService) {
    this.activeProjectId=this.api.projectId;
    if(this.api.loggedIn())this.checkSession();
  }
  title(){return this.menu.find(item=>item.key===this.screen)?.label||'Inicio';}
  login(){this.error='';this.api.saveCredentials(this.email,this.password);this.checkSession();}
  checkSession(){this.api.get('/auth/me').subscribe({next:(user:any)=>{this.email=user.usuario;this.role=user.rol;this.admin=this.role==='ROLE_ADMIN';this.logged=true;this.password='';this.loadProjects();},error:(e:any)=>{this.api.logout();this.logged=false;this.error=errorMessage(e);}});}
  loadProjects(){this.api.get('/proyectos').subscribe({next:(v:any)=>{this.projects=v; if(!v.some((p:any)=>p.id===this.activeProjectId)&&v.length){this.chooseProject(v[0].id);}},error:(e:any)=>this.error=errorMessage(e)});}
  onProjectCreated(id:number){this.api.chooseProject(id);this.activeProjectId=id;this.loadProjects();}
  chooseProject(id:number){this.activeProjectId=Number(id);this.api.chooseProject(this.activeProjectId);this.screen='projects';}
  logout(){this.api.logout();this.logged=false;this.password='';this.role='';}
}
