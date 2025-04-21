import { Component } from '@angular/core';
import { HeaderComponent } from "./CommonComponents/header/header.component";
import { FooterComponent } from "./CommonComponents/footer/footer.component";
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [HeaderComponent, FooterComponent, RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {}
