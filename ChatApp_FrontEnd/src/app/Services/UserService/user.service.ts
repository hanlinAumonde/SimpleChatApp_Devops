import { HttpClient} from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { UserModel } from '../../Models/UserModel';
import properties from '../../properties.json';
import { Page } from '../../Models/PageableModel';
import { Router } from '@angular/router';
import { ChatroomInfo } from '../../Models/ChatroomModel';

@Injectable({
  providedIn: 'root'
})
export class UserService {

  UserApi = properties.SpringServerUrl + properties.UserApi.BaseUrl;

  constructor(private httpClient: HttpClient, private router : Router){}

  getOtherUsers(page:number) : Observable<Page<UserModel>>{
    return this.httpClient.get<Page<UserModel>>(
      this.UserApi + properties.UserApi.AllOtherUsers,
      {
        params: { page: page.toString() },
        withCredentials: true
      }
    );
  }

  getChatroomsByUser(userId: number, isOwnedByUser: boolean, page: number) : Observable<Page<ChatroomInfo>>{
    return this.httpClient.get<Page<ChatroomInfo>>(
      this.UserApi + '/' + userId + (isOwnedByUser? properties.UserApi.OwnedChatrooms : properties.UserApi.JoinedChatrooms),
      {
        params: { page: page.toString() },
        withCredentials: true
      }
    );
  }
}
