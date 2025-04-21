// import { Injectable } from '@angular/core';
// import {HttpEvent, HttpHandler, HttpInterceptor, HttpRequest} from '@angular/common/http';
// import {Observable} from 'rxjs';
// import {CheckLoginService} from '../../Services/CheckLogin/check-login.service';
// import properties from '../../properties.json';

// @Injectable({
//   providedIn: 'root'
// })
// export class AuthHeaderInterceptorService implements HttpInterceptor {

//   constructor(private checkLoginService : CheckLoginService) { }

//   intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
//     const token = this.checkLoginService.getAuthToken();
//     if(token && !(req.url.includes(properties.LoginApi.BaseUrl) &&
//                   !(req.url.includes(properties.LoginApi.LoggedUser) || req.url.includes(properties.LoginApi.Logout))
//                   )
//     ){
//       const authReq = req.clone({
//         headers : req.headers.set("Authorization", "Bearer " + token)
//       });
//       return next.handle(authReq);
//     }
//     return next.handle(req);
//   }
// }
