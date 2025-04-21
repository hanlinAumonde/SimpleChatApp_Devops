export interface UserModel {
    id: number;
    firstName: string;
    lastName: string;
    mail: string;
}

export interface UserInChatroomModel extends UserModel {
    isConnecting:0|1;
}

export interface CreateCompteModel {
    firstName: string;
    lastName: string;
    mail: string;
    password: string;
    createMsg: "create compte" | "compte already exists" | null;
}