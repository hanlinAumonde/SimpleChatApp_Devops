import { CanActivateFn, Router } from '@angular/router';
import { CheckLoginService } from '../Services/CheckLogin/check-login.service';
import { inject } from '@angular/core';
import routerLinkList from '../routerLinkList.json';
import { SharedUserInfoService } from '../Services/shared/User/shared-user-info.service';
import { tap, map } from 'rxjs/operators';

export const redirectGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  const checkLoginService = inject(CheckLoginService);
  const sharedUserInfoService = inject(SharedUserInfoService);

  return checkLoginService.getLoggedUser().pipe(
    tap(userInfo => {
      if (userInfo.id !== 0) {
        sharedUserInfoService.emitUserInfo(userInfo);
        router.navigate([routerLinkList[0].path]);
      } else {
        router.navigate([routerLinkList[6].path]);
      }
    }),
    map(() => false)
  );
};

export const nonAuthGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  const checkLoginService = inject(CheckLoginService);
  const sharedUserInfoService = inject(SharedUserInfoService);

  return checkLoginService.getLoggedUser().pipe(
    tap(userInfo => {
      if (userInfo.id !== 0) {
        sharedUserInfoService.emitUserInfo(userInfo);
        router.navigate([routerLinkList[0].path]);
      }
    }),
    map(userInfo => userInfo.id === 0)
  );
};

export const authGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  const checkLoginService = inject(CheckLoginService);
  const sharedUserInfoService = inject(SharedUserInfoService);

  return checkLoginService.getLoggedUser().pipe(
    tap(userInfo => {
      if (userInfo.id === 0) {
        router.navigate([routerLinkList[6].path]);
      } else {
        sharedUserInfoService.emitUserInfo(userInfo);
      }
    }),
    map(userInfo => userInfo.id !== 0)
  );
};

export const tokenParamGuard: CanActivateFn = (route, state) => {
  const token = route.queryParams['token'];
  const router = inject(Router);
  const checkLoginService = inject(CheckLoginService);

  if(token === undefined) {
    router.navigate([routerLinkList[6].path]);
    return false;
  }else{
    return checkLoginService.checkValidToken(token).pipe(
      tap(isValid => {
        if(!isValid){
          router.navigate([routerLinkList[6].path]);
        }
      }),
      map(isValid => isValid)
    );
  }
};
