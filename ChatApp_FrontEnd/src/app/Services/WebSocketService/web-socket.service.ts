// import { Injectable } from '@angular/core';
// import { webSocket, WebSocketSubject } from 'rxjs/webSocket';
// import { Router } from '@angular/router';
// import { Subject, EMPTY } from 'rxjs';
// import { catchError, tap, finalize } from 'rxjs/operators';
// import properties from '../../properties.json';
// import routerLinkList from '../../routerLinkList.json';
// import { InitialMessage } from '../../Models/ChatMessage';
//
// @Injectable({
//   providedIn: 'root'
// })
// export class WebSocketService {
//   private webSocket$!: WebSocketSubject<any>;
//   private messageSubject = new Subject<InitialMessage>();
//   public message$ = this.messageSubject.asObservable();
//   private isConnected = false;
//
//   constructor(private router: Router) {
//   }
//
//   connectToWebSocket(chatroomId: number, userId: number): void {
//     const wsUrl = properties.WebSocketApi + window.location.host + "/ws/chatroom/" + chatroomId + "/user/" + userId;
//
//     if (this.webSocket$) {
//       this.webSocket$.complete();
//     }
//
//     this.webSocket$ = webSocket({
//       url: wsUrl,
//       openObserver: {
//         next: () => {
//           console.log('WebSocket connection opened');
//           this.isConnected = true;
//         }
//       },
//       closeObserver: {
//         next: () => {
//           console.log('WebSocket connection closed');
//           this.isConnected = false;
//           // this.router.navigate([routerLinkList[0].path]);
//         }
//       }
//     });
//
//     this.webSocket$.pipe(
//       tap((message: any) => {
//         this.messageSubject.next(message);
//       }),
//       catchError((error) => {
//         console.error('WebSocket error: ', error);
//         this.isConnected = false;
//         this.router.navigate([routerLinkList[0].path]);
//         return EMPTY;
//       }),
//       finalize(() => {
//         this.isConnected = false;
//         console.log('WebSocket connection finalized');
//       })
//     ).subscribe();
//   }
//
//   get webSocketClientState(): number {
//     if (!this.webSocket$) {
//       return WebSocket.CONNECTING; // 0
//     }
//     return this.isConnected ? WebSocket.OPEN : WebSocket.CLOSED; // 1 : 3
//   }
//
//   sendMessage(message: any): void {
//     if (this.webSocket$ && this.isConnected) {
//       this.webSocket$.next(message); // RxJS WebSocket 会自动进行 JSON 序列化
//     } else {
//       console.warn('WebSocket is not connected. Cannot send message.');
//     }
//   }
//
//   closeWebSocket(): void {
//     if (this.webSocket$) {
//       this.webSocket$.complete();
//       this.isConnected = false;
//     }
//   }
// }
import { Injectable } from '@angular/core';
import properties from '../../properties.json';
import routerLinkList from '../../routerLinkList.json';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { InitialMessage } from '../../Models/ChatMessage';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
  private webSocketClient!: WebSocket;
  private messageSubject = new Subject<InitialMessage>();
  public message$ = this.messageSubject.asObservable();

  constructor(private router: Router) {}

  connectToWebSocket(chatroomId: number, userId: number): void {
    this.webSocketClient = new WebSocket(properties.WebSocketApi + window.location.host + "/ws/chatroom/" + chatroomId + "/user/" + userId);
    this.webSocketClient.onopen = (event) => console.log("WebSocket connection opened");
    this.webSocketClient.onclose = (event) => {
      console.log("WebSocket connection closed");
      //this.router.navigate([routerLinkList[0].path]);
    };
    this.webSocketClient.onerror = (event) => {
      console.error("WebSocket error: ", event);
      this.router.navigate([routerLinkList[0].path]);
    }
    this.webSocketClient.onmessage = (event) => {
      this.messageSubject.next(JSON.parse(event.data));
    }
  }

  get webSocketClientState(): number {
    return this.webSocketClient.readyState;
  }

  sendMessage(message: any): void {
    this.webSocketClient.send(JSON.stringify(message));
  }

  closeWebSocket(): void {
    this.webSocketClient.close();
  }
}
