export interface StartOptions {
    orderId: string;
    title?: string;
    text?: string;
    subText?: string;
    style?: string;
    silent?: boolean;
    progress?: number;            // 0..100
    etaSeconds?: number;
    accentColor?: string;         // "#RRGGBB"
    smallIcon?: string;           // Android: mipmap/drawable name
    largeIcon?: string;           // Android: mipmap/drawable name
    channelId?: string;           // default: "active_progress"
    ongoing?: boolean;            // default: true (Android)
    indeterminate?: boolean;      // default: false (Android)
    payload?: Record<string, any>;
}

export interface UpdateOptions {
    orderId: string;
    progress?: number;
    etaSeconds?: number;
    title?: string;
    text?: string;
    payload?: Record<string, any>;
}

export interface StopOptions {
    orderId: string;
    reason?: 'arrived' | 'canceled' | 'timeout' | 'other';
    text?: string;
}

export interface GetIosActivityPushTokenResult {
    orderId: string;
    activityToken?: string; // base64 APNs Live Activity push token
}

export interface ActiveProgressPlugin {
    start(options: StartOptions): Promise<void>;
    update(options: UpdateOptions): Promise<void>;
    stop(options: StopOptions): Promise<void>;

    // iOS only
    getIosActivityPushToken(orderId: string): Promise<GetIosActivityPushTokenResult>;
}
