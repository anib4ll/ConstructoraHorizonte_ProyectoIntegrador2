import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly base = 'http://localhost:8080/api';
  projectId = Number(sessionStorage.getItem('horizonteProjectId') || 1);
  chooseProject(id: number) { this.projectId = Number(id); sessionStorage.setItem('horizonteProjectId', String(id)); }
  constructor(private http: HttpClient) {}

  saveCredentials(email: string, password: string) {
    sessionStorage.setItem('horizonteAuth', btoa(email + ':' + password));
  }
  logout() { sessionStorage.removeItem('horizonteAuth'); }
  loggedIn(): boolean { return !!sessionStorage.getItem('horizonteAuth'); }
  private options() {
    const credentials = sessionStorage.getItem('horizonteAuth') || '';
    return { headers: new HttpHeaders({ Authorization: 'Basic ' + credentials }) };
  }
  uploadExcel(path: string, file: File): Observable<any> { const form = new FormData(); form.append("archivo",file); return this.http.post(this.base + path,form,this.options()); }
  get(path: string): Observable<any> { return this.http.get(this.base + path, this.options()); }
  post(path: string, data: any = {}): Observable<any> { return this.http.post(this.base + path, data, this.options()); }
  put(path: string, data: any): Observable<any> { return this.http.put(this.base + path, data, this.options()); }
  patch(path: string, data: any): Observable<any> { return this.http.patch(this.base + path, data, this.options()); }
  delete(path: string): Observable<any> { return this.http.delete(this.base + path, this.options()); }
}
