import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Page } from '../../Models/PageableModel';
import { ModifyChatroomModel } from '../../Models/ChatroomModel';
import properties from '../../properties.json';
import { Observable } from 'rxjs';
import { ModifiedChatroomModel, NewChatroomModel } from '../../Models/NewChatroomModel';
import { UserModel } from '../../Models/UserModel';
import { HistoryMessage } from '../../Models/ChatMessage';

@Injectable({
  providedIn: 'root'
})
export class ChatroomService {

  ChatroomApi = properties.SpringServerUrl + properties.ChatroomApi.BaseUrl;

  constructor(private httpClient: HttpClient) {}

  addChatroom(chatroom: NewChatroomModel): Observable<boolean>{
    return this.httpClient.post<boolean>(
      this.ChatroomApi + properties.ChatroomApi.CreateChatroom,
      chatroom,
      {withCredentials: true}
    );
  }

  deleteChatroom(chatroomId: number): Observable<boolean>{
    return this.httpClient.delete<boolean>(
      this.ChatroomApi + '/' + chatroomId,
      {withCredentials: true}
    );
  }

  leaveChatroom(chatroomId: number, userId: number): Observable<boolean>{
    return this.httpClient.delete<boolean>(
      this.ChatroomApi + '/' + chatroomId + properties.ChatroomApi.UsersInvited + '/' + userId,
      {withCredentials: true}
    );
  }

  getChatroomForModify(chatroomId: number): Observable<ModifyChatroomModel>{
    return this.httpClient.get<ModifyChatroomModel>(
      this.ChatroomApi + '/' + chatroomId,
      {withCredentials: true}
    );
  }

  getUsersInvited(chatroomId:number, page:number) : Observable<Page<UserModel>>{
    return this.httpClient.get<Page<UserModel>>(
      this.ChatroomApi + '/' + chatroomId + properties.ChatroomApi.UsersInvited,
      {
        params: { page: page.toString() },
        withCredentials: true
      }
    );
  }

  getUsersNotInvited(chatroomId:number, page:number) : Observable<Page<UserModel>>{
    return this.httpClient.get<Page<UserModel>>(
      this.ChatroomApi + '/' + chatroomId + properties.ChatroomApi.UsersNotInvited,
      {
        params: { page: page.toString() },
        withCredentials: true
      }
    );
  }

  modifyChatroom(chatroomId: number, modifiedChatroom: ModifiedChatroomModel): Observable<boolean>{
    return this.httpClient.put<boolean>(
      this.ChatroomApi + '/' + chatroomId,
      modifiedChatroom,
      {withCredentials: true}
    )
  }

  getAllUsersInChatroom(chatroomId: number): Observable<UserModel[]>{
    return this.httpClient.get<UserModel[]>(
      this.ChatroomApi + '/' + chatroomId + properties.ChatroomApi.Members,
      {withCredentials: true}
    );
  }

  getHistoryMessages(chatroomId: number): Observable<HistoryMessage[]>{
    return this.httpClient.get<HistoryMessage[]>(
      this.ChatroomApi + '/' + chatroomId + properties.ChatroomApi.HistoryMessages,
      {withCredentials: true}
    )
  }

  getHistoryMessagesByPage(chatroomId: number, page: number): Observable<HistoryMessage[]>{
    return this.httpClient.get<HistoryMessage[]>(
      this.ChatroomApi + '/' + chatroomId + properties.ChatroomApi.HistoryMessages,
      {
        params: { page: page.toString() },
        withCredentials: true
      }
    );
  }
}
