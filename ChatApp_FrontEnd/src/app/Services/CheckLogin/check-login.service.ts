import { Injectable } from '@angular/core';
import { catchError, finalize, map, Observable, of, tap } from 'rxjs';
import { CreateCompteModel, UserModel } from '../../Models/UserModel';
import properties from '../../properties.json';
import { HttpClient } from '@angular/common/http';
import routerLinkList from '../../routerLinkList.json';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class CheckLoginService {

  LoginApi = properties.SpringServerUrl + properties.LoginApi.BaseUrl;
  routerLinkList = routerLinkList;

  constructor(private httpClient: HttpClient, private router: Router) {}

  getLoggedUser() : Observable<UserModel>{
    return this.httpClient.get<UserModel>(
      this.LoginApi + properties.LoginApi.LoggedUser,
      {withCredentials: true}
    );
  }

  userLogin(loginInfo: FormData): Observable<any>{
    return this.httpClient.post<any>(
      this.LoginApi + properties.LoginApi.LoginProcess,
      loginInfo,
      {withCredentials: true}
    );
  }

  getVerificationCode(email: string): Observable<any> {
    return this.httpClient.get<any>(
      this.LoginApi + properties.LoginApi.DemandVerificationCode,
      {
        params: { email: email },
        withCredentials: true
      }
    );
  }

  userLoginWithVerificationCode(loginInfo: FormData): Observable<any> {
    return this.httpClient.post<any>(
      this.LoginApi + properties.LoginApi.LoginWithVerificationCode,
      loginInfo,
      {withCredentials: true}
    );
  }

  // setAuthToken(token: string) : void {
  //   localStorage.setItem("auth_token", token);
  // }

  // getAuthToken(): string | null {
  //   return localStorage.getItem('auth_token');
  // }

  // removeAuthToken(): void {
  //   localStorage.removeItem('auth_token');
  // }

  sendResetPwdEmail(email: FormData) : Observable<any> {
    return this.httpClient.post<any>(
      this.LoginApi + properties.LoginApi.SendResetPwdEmail,
      email,
      {withCredentials: true}
    );
  }

  checkValidToken(token: string) : Observable<boolean> {
    return this.httpClient.get<boolean>(
      this.LoginApi + properties.LoginApi.ValidateToken,
      {
        params: { token: token },
        withCredentials: true
      }
    );
  }

  resetPassword(formData: FormData) : Observable<boolean> {
    return this.httpClient.put<boolean>(
      this.LoginApi + properties.LoginApi.ResetPassword,
      formData,
      {withCredentials: true}
    );
  }

  createCompte(compteInfo:any): Observable<CreateCompteModel> {
    return this.httpClient.post<CreateCompteModel>(
      this.LoginApi + properties.LoginApi.CreateCompte,
      compteInfo,
      {withCredentials: true}
    );
  }

  onLoggout() : Observable<any> {
    return this.httpClient.post(
      this.LoginApi + properties.LoginApi.Logout,
      null,
      {withCredentials: true}
    )
    .pipe(
      tap(() => {
        //this.removeAuthToken();
        console.log('Logout successful');
      }),
      map(() => {
        return true;
      }),
      catchError((error) => {
        console.error('Logout failed:', error);
        return of(false);
      }),
      finalize(() => {
        this.router.navigate([routerLinkList[6].path]);
      })
    );
  }
}
