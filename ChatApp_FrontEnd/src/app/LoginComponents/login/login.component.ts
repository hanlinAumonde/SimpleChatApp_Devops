import { Component, ViewChild } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CheckLoginService } from '../../Services/CheckLogin/check-login.service';
import routerLinkList from '../../routerLinkList.json';
import { SharedUserInfoService } from '../../Services/shared/User/shared-user-info.service';
import { UserModel } from '../../Models/UserModel';
import { map, Observable } from 'rxjs';
import { AsyncPipe } from '@angular/common';
import { ValidatorsService } from '../../Services/ValidatorService/validators.service';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink ,AsyncPipe],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  @ViewChild('formDir') formDir: any;
  loginForm! : FormGroup;

  afterGettedUserInfo$!: Observable<string>;

  loginWithCode : boolean = false;

  routerLinkList = routerLinkList;

  restTime = 0;
  timer!: any;

  constructor(private formBuilder: FormBuilder,
              private checkLoginService: CheckLoginService,
              private sharedUserInfoService: SharedUserInfoService,
              private validatorsService: ValidatorsService,
              private router: Router)
  {
    this.loginForm = this.formBuilder.group({
      username: ['',[Validators.required, this.validatorsService.emailValidator()]],
      password: ['',Validators.required],
      rememberMe: [false]
    });
  }

  get username(): FormControl { return this.loginForm.get('username') as FormControl; }

  get password(): FormControl { return this.loginForm.get('password') as FormControl; }

  resetLoginForm(): void {
    this.loginForm.reset();
    this.formDir.resetForm();
    this.afterGettedUserInfo$ = new Observable();
    this.restTime = 0;
    clearInterval(this.timer);
  }

  changeLoginType(): void {
    this.loginWithCode = !this.loginWithCode;
    this.resetLoginForm();
  }

  onSendCode(): void {
    const email = this.loginForm.get('username')?.value;
    if (email) {
      clearInterval(this.timer);
      this.restTime = 60;
      this.timer = setInterval(() => {
        if (this.restTime > 0) {
          this.restTime--;
        }
      }, 1000);
      this.checkLoginService.getVerificationCode(email).pipe(
        map(response => {
          if(response.status === "error"){
            window.alert(response.msg);
          }
        })
      ).subscribe();
    }else{
      window.alert("Please enter your email address to receive the verification code.");
    }
  }

  onSubmit(): void {
    console.log(this.loginForm.value);
    const formData = new FormData();
    formData.append('username', this.loginForm.value.username);
    formData.append((this.loginWithCode? 'verification-code' : 'password'), this.loginForm.value.password);
    formData.append('remember-me', this.loginForm.value.rememberMe);

    this.afterGettedUserInfo$ =
    (this.loginWithCode? this.checkLoginService.userLoginWithVerificationCode(formData) 
                        : this.checkLoginService.userLogin(formData))
    .pipe(
      map(response => {
        if(response.status === "success"){
          console.log(response.message);
          //this.checkLoginService.setAuthToken(response.LoginToken);
          this.sharedUserInfoService.emitUserInfo(response.UserInfo as UserModel);
          this.router.navigate([routerLinkList[0].path]);
        }else{
          return response.msg;
        }
      })
    )
  }
}
