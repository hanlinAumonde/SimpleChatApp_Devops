import {Injectable} from '@angular/core';
import properties from '../../properties.json'
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {HistoryMessage} from '../../Models/ChatMessage';

@Injectable({
  providedIn: "root"
})
export class HistoryMessageService {
  MessageApi = properties.SpringServerUrl + properties.MessageApi.BaseUrl;

  constructor(private httpClient: HttpClient) {}

  getHistoryMessages(chatroomId: number): Observable<HistoryMessage[]>{
    return this.httpClient.get<HistoryMessage[]>(
      this.MessageApi + '/chatrooms/' + chatroomId + properties.MessageApi.HistoryMessages,
      {withCredentials: true}
    )
  }

  getHistoryMessagesByPage(chatroomId: number, page: number): Observable<HistoryMessage[]>{
    return this.httpClient.get<HistoryMessage[]>(
      this.MessageApi + '/chatrooms/' + chatroomId + properties.MessageApi.HistoryMessages,
      {
        params: { page: page.toString() },
        withCredentials: true
      }
    );
  }
}
