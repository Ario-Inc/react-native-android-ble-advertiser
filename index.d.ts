export as namespace AndroidBLEAdvertiserModule;

export function setCompanyId(companyId: number): void;
export function sendPacket(uid: String, payload: number[]): Promise<void>;
export function cancelPacket(uid: String): Promise<void>;
export function cancelAllPackets(): Promise<void>;
