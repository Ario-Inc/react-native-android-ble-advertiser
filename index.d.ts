export as namespace AndroidBLEAdvertiserModule;

export function setCompanyId(companyId: number): void;
export function sendPacket(uid: String, payload: number[]): Promise<boolean>;
export function cancelPacket(uid: String): void;
export function cancelAllPackets(): void;
export function toggleAdapter(): Promise<void>;