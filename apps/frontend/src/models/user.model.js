export class UserResponse {
  constructor({
    id = null,
    username = '',
    fullName = '',
    email = '',
    phone = '',
    role = 'CUSTOMER',
    status = true,
    dateOfBirth = '',
    gender = true,
  } = {}) {
    this.id = id;
    this.username = username;
    this.fullName = fullName;
    this.email = email;
    this.phone = phone;
    this.role = role;
    this.status = status;
    this.dateOfBirth = dateOfBirth;
    this.gender = gender;
  }
}

export class CustomerResponse {
  constructor({
    id = null,
    userId = null,
    fullName = '',
    phone = '',
    email = '',
    address = '',
    loyaltyPoints = 0,
    walletBalance = 0,
  } = {}) {
    this.id = id;
    this.userId = userId;
    this.fullName = fullName;
    this.phone = phone;
    this.email = email;
    this.address = address;
    this.loyaltyPoints = loyaltyPoints;
    this.walletBalance = walletBalance;
  }
}

export class UpdateProfileRequest {
  constructor({
    fullName = '',
    email = '',
    phone = '',
    dateOfBirth = '',
    gender = true,
  } = {}) {
    this.fullName = fullName;
    this.email = email;
    this.phone = phone;
    this.dateOfBirth = dateOfBirth;
    this.gender = gender;
  }
}
