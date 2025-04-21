import { Component, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CheckLoginService } from '../../Services/CheckLogin/check-login.service';
import { RouterLink } from '@angular/router';
import { map, Observable } from 'rxjs';
import { AsyncPipe } from '@angular/common';
import routerLinkList from '../../routerLinkList.json';
import { ValidatorsService } from '../../Services/ValidatorService/validators.service';

@Component({
  selector: 'ForgetPassword',
  imports: [ReactiveFormsModule, AsyncPipe, RouterLink],
  templateUrl: './forget-password.component.html',
  styleUrl: './forget-password.component.css'
})
export class ForgetPasswordComponent {
  @ViewChild('formDir') formDir: any;
  forgetPwdForm!: FormGroup;

  routerLinkList = routerLinkList;

  afterClickedSubmit$!: Observable<any>;

  restTime = 60;
  timer!: any;

  constructor(private formBuilder: FormBuilder,
              private checkLoginService: CheckLoginService,
              private validateService: ValidatorsService) 
  {
    this.forgetPwdForm = this.formBuilder.group({
      email: ['', [Validators.required, this.validateService.emailValidator()]]
    });
  }

  get email() { return this.forgetPwdForm.get('email'); }

  resetForgetPwdForm(): void {
    this.forgetPwdForm.reset();
    this.formDir.resetForm();
    this.afterClickedSubmit$ = new Observable();
    this.restTime = 60;
    clearInterval(this.timer);
  }

  onSubmit(): void {
    const formData = new FormData();
    formData.append('email', this.forgetPwdForm.value.email);

    // Send email to the user with the password reset link
    this.afterClickedSubmit$ = this.checkLoginService.sendResetPwdEmail(formData).pipe(
      map(response => {
        this.timer = setInterval(() => {
          if (this.restTime > 0) {
            this.restTime--;
          }
        }, 1000);
        return response;
      })
    );
  }
}
