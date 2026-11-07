export class LoginRequest {
  constructor({ username = '', password = '' } = {}) {
    this.username = username;
    this.password = password;
  }
}

export class RegisterRequest {
  constructor({
    username = '',
    password = '',
    fullName = '',
    email = '',
    phone = '',
    dateOfBirth = '',
    gender = true,
  } = {}) {
    this.username = username;
    this.password = password;
    this.fullName = fullName;
    this.email = email;
    this.phone = phone;
    this.dateOfBirth = dateOfBirth;
    this.gender = gender;
  }
}

export class LoginResponse {
  constructor({
    token = '',
    refreshToken = '',
    expiresIn = 0,
    user = null,
  } = {}) {
    this.token = token;
    this.refreshToken = refreshToken;
    this.expiresIn = expiresIn;
    this.user = user;
  }
}

export class RefreshTokenRequest {
  constructor({ refreshToken = '' } = {}) {
    this.refreshToken = refreshToken;
  }
}

export class ForgotPasswordRequest {
  constructor({ email = '' } = {}) {
    this.email = email;
  }
}

export class ResetPasswordRequest {
  constructor({ token = '', newPassword = '', confirmPassword = '' } = {}) {
    this.token = token;
    this.newPassword = newPassword;
    this.confirmPassword = confirmPassword;
  }
}

export class ChangePasswordRequest {
  constructor({ oldPassword = '', newPassword = '', confirmPassword = '' } = {}) {
    this.oldPassword = oldPassword;
    this.newPassword = newPassword;
    this.confirmPassword = confirmPassword;
  }
}
