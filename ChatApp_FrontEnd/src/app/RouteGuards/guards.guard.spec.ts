import { TestBed } from '@angular/core/testing';
import { CanActivateFn } from '@angular/router';
import { authGuard, nonAuthGuard, redirectGuard } from './guards.guard';


describe('guardsGuard', () => {
  const executeRedirectGuard: CanActivateFn = (...guardParameters) => 
      TestBed.runInInjectionContext(() => redirectGuard(...guardParameters));

  const executeNonAuthGuard: CanActivateFn = (...guardParameters) =>
      TestBed.runInInjectionContext(() => nonAuthGuard(...guardParameters));

  const executeAuthGuard: CanActivateFn = (...guardParameters) =>
      TestBed.runInInjectionContext(() => authGuard(...guardParameters));

  beforeEach(() => {
    TestBed.configureTestingModule({});
  });

  it('should be created', () => {
    expect(executeRedirectGuard).toBeTruthy();
    expect(executeNonAuthGuard).toBeTruthy();
    expect(executeAuthGuard).toBeTruthy();
  });
});
