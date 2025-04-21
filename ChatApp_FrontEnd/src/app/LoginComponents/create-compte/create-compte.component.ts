import { AsyncPipe } from '@angular/common';
import { Component, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { map, Observable, tap } from 'rxjs';
import { ValidatorsService } from '../../Services/ValidatorService/validators.service';
import { CreateCompteModel, UserModel } from '../../Models/UserModel';
import { CheckLoginService } from '../../Services/CheckLogin/check-login.service';
import { SharedUserInfoService } from '../../Services/shared/User/shared-user-info.service';
import routerLinkList from '../../routerLinkList.json';
import { Router } from '@angular/router';

@Component({
  selector: 'CreateCompte',
  imports: [ReactiveFormsModule, AsyncPipe],
  templateUrl: './create-compte.component.html',
  styleUrl: './create-compte.component.css'
})
export class CreateCompteComponent {
  @ViewChild('formDir') formDir: any;
  createCompteForm!: FormGroup;

  afterCreateCompte$!: Observable<CreateCompteModel>;
  login$!: Observable<string>;

  constructor(private formBuilder: FormBuilder, 
              private router: Router,
              private checkLoginService: CheckLoginService,
              private sharedUserInfoService: SharedUserInfoService,
              private validatorsService: ValidatorsService) {
    this.createCompteForm = this.formBuilder.group({
      firstName: ['', [Validators.required,Validators.minLength(2),Validators.maxLength(50),this.validatorsService.First_LastNameValidator()]],
      lastName: ['', [Validators.required,Validators.minLength(2),Validators.maxLength(50),this.validatorsService.First_LastNameValidator()]],
      mail: ['', [Validators.required, this.validatorsService.emailValidator()]],
      password: ['', [Validators.required, this.validatorsService.passwordFormatValidator()]],
      confirmPassword: ['', [Validators.required, this.validatorsService.confirmPasswordValidator('password')]]
    });
  }

  get firstName() { return this.createCompteForm.get('firstName'); }

  get lastName() { return this.createCompteForm.get('lastName'); }

  get mail() { return this.createCompteForm.get('mail'); }

  get password() { return this.createCompteForm.get('password'); }

  get confirmPassword() { return this.createCompteForm.get('confirmPassword'); }

  onSubmit(): void {
    const createCompte : CreateCompteModel = {
      firstName: this.createCompteForm.value.firstName,
      lastName: this.createCompteForm.value.lastName,
      mail: this.createCompteForm.value.mail,
      password: this.createCompteForm.value.password,
      createMsg: null
    };

    this.afterCreateCompte$ = this.checkLoginService.createCompte(createCompte).pipe(
      tap(response => {
        const loginForm = new FormData();
        loginForm.append('username', response.mail);
        loginForm.append('password', response.password);
        this.login$ = this.checkLoginService.userLogin(loginForm).pipe(
          map(response => {
            if(response.status === "success"){
              this.sharedUserInfoService.emitUserInfo(response.UserInfo as UserModel);
              this.router.navigate([routerLinkList[0].path]);
            }else{
              return response.msg;
            }
          })
        );
      }),
      map(response => response)
    );
  }

  returnToLogin() {
    this.router.navigate([routerLinkList[6].path]);
  }

  resetCreateCompteForm() {
    this.formDir.resetForm();
    this.createCompteForm.reset();
    this.afterCreateCompte$ = new Observable();
    this.login$ = new Observable();
  }
}
