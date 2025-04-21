import { Routes } from '@angular/router';
import { AcceuilComponent } from './ChatAppComponents/Pages/acceuil/acceuil.component';
import { CreateChatroomComponent } from './ChatAppComponents/Pages/create-chatroom/create-chatroom.component';
import { ChatroomComponent } from './ChatAppComponents/Pages/chatroom/chatroom.component';
import { JoinedChatroomsListComponent } from './ChatAppComponents/Pages/joined-chatrooms-list/joined-chatrooms-list.component';
import { OwnedChatroomsListComponent } from './ChatAppComponents/Pages/owned-chatrooms-list/owned-chatrooms-list.component';
import { ModifyChatroomComponent } from './ChatAppComponents/Pages/modify-chatroom/modify-chatroom.component';
import { PageNotFoundComponent } from './ChatAppComponents/Pages/page-not-found/page-not-found.component';
import routerLinkList from './routerLinkList.json'
import { LoginContainerComponent } from './LoginComponents/login-container/login-container.component';
import { ForgetPasswordComponent } from './LoginComponents/forget-password/forget-password.component';
import { LoginComponent } from './LoginComponents/login/login.component';
import { MainComponent } from './ChatAppComponents/main/main.component';
import { authGuard, nonAuthGuard, redirectGuard, tokenParamGuard } from './RouteGuards/guards.guard';
import { ResetPasswordComponent } from './LoginComponents/reset-password/reset-password.component';
import { CreateCompteComponent } from './LoginComponents/create-compte/create-compte.component';

export const routes: Routes = [
    {
      path: '',
      canActivate: [redirectGuard],
      component: LoginComponent,
      pathMatch: 'full'
    },
    {
      path: '',
      component: LoginContainerComponent,
      canActivate: [nonAuthGuard],
      children: [
        {path: routerLinkList[6].pathRouter, component: LoginComponent},
        {path: routerLinkList[7].pathRouter, component: ForgetPasswordComponent},
        {
          path: routerLinkList[8].pathRouter, 
          component: ResetPasswordComponent,
          canActivate: [tokenParamGuard]
        },
        {path: routerLinkList[9].pathRouter, component: CreateCompteComponent}
      ]
    },
    {
      path: '',
      component: MainComponent,
      canActivate: [authGuard],
      children: [
        {path: routerLinkList[0].pathRouter, component: AcceuilComponent},
        {path: routerLinkList[1].pathRouter, component: CreateChatroomComponent},
        {path: routerLinkList[3].pathRouter, component: JoinedChatroomsListComponent},
        {path: routerLinkList[2].pathRouter, component: OwnedChatroomsListComponent},
        {path: routerLinkList[4].pathRouter, component: ModifyChatroomComponent},
        {path: routerLinkList[5].pathRouter, component: ChatroomComponent},
        {path: '', redirectTo: routerLinkList[0].pathRouter, pathMatch: 'full'},
        {path: '**', component: PageNotFoundComponent}
      ]
    },
    {path: '**', component: PageNotFoundComponent}
];
