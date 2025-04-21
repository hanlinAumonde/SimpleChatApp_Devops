import { Component } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CheckLoginService } from '../../Services/CheckLogin/check-login.service';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ValidatorsService } from '../../Services/ValidatorService/validators.service';
import routerLinkList from '../../routerLinkList.json';
import { Observable} from 'rxjs';
import { AsyncPipe } from '@angular/common';

@Component({
  selector: 'app-reset-password',
  imports: [ReactiveFormsModule, AsyncPipe, RouterLink],
  templateUrl: './reset-password.component.html',
  styleUrl: './reset-password.component.css'
})
export class ResetPasswordComponent {
  
  token!: string;

  resetPasswordForm!: FormGroup;

  afterSubmitPwdReset$!: Observable<any>;

  routerLinkList = routerLinkList;

  constructor(private route: ActivatedRoute,
              private checkLoginService: CheckLoginService,
              private validatorsService: ValidatorsService,
              private formBuilder: FormBuilder
  ) {
    this.route.queryParams.subscribe(params => {
      this.token = params['token'];
    });
    this.resetPasswordForm = this.formBuilder.group({
      password: ['', [Validators.required, 
                      this.validatorsService.passwordFormatValidator()]],
      confirmPassword: ['', [Validators.required, 
                             this.validatorsService.confirmPasswordValidator('password')]]
    });
  }

  get password() { return this.resetPasswordForm.get('password'); }

  get confirmPassword() { return this.resetPasswordForm.get('confirmPassword'); }

  onSubmit() {
    const formData = new FormData();
    formData.append('token', this.token);
    formData.append('password', this.password?.value);

    this.afterSubmitPwdReset$ = this.checkLoginService.resetPassword(formData);
  }
}
