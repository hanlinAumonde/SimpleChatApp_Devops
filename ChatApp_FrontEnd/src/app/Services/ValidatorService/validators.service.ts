import { Injectable } from '@angular/core';
import { AbstractControl, ValidationErrors } from '@angular/forms';

@Injectable({
  providedIn: 'root'
})
export class ValidatorsService {

  constructor() {}

  specialCharValidator() {
    return (control: AbstractControl): ValidationErrors | null => {
      const regex = /[<>/\\{}[\]()=+*?!@#$%^&|~`;]/;
      const invalid = regex.test(control.value);
      return invalid ? { specialChars: true } : null;
    };
  }

  First_LastNameValidator() {
    return (control: AbstractControl): ValidationErrors | null => {
      const regex = /^[A-Z]*(-[A-Z]*)?$/;
      const invalid = !regex.test(control.value);
      return invalid ? { invalidName: true } : null;
    };
  }

  emailValidator() {
    return (control: AbstractControl): ValidationErrors | null => {
      const regex = /^[A-Za-z0-9]+([_\.][A-Za-z0-9]+)*@([A-Za-z0-9\-]+\.)+[A-Za-z]{2,6}$/;
      const invalid = !regex.test(control.value);
      return invalid ? { invalidEmail: true } : null;
    };
  }

  passwordFormatValidator() {
    return (control: AbstractControl): ValidationErrors | null => {
      const regex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[a-zA-Z\d]{8,15}$/;
      const invalid = !regex.test(control.value);
      return invalid ? { invalidPasswordFormat: true } : null;
    };
  }

  confirmPasswordValidator(passwordControlName: string) {
    return (control: AbstractControl): ValidationErrors | null => {
      const passwordControl = control.parent?.get(passwordControlName);
      const invalid = passwordControl?.value !== control.value;
      return invalid ? { inConfirmedPassword: true } : null;
    };
  }

  dateFormatValidator() {
    return (control: AbstractControl): ValidationErrors | null => {
      const dateStringFormat = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/;
      const invalid = !dateStringFormat.test(control.value);
      return invalid ? { invalidDateFormat: true } : null;
    };
  }
}
