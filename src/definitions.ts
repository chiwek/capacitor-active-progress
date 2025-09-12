export interface StartOptions {
    orderId: string;
    title?: string;
    text?: string;
    progress?: number;            // 0..100
    etaSeconds?: number;
    accentColor?: string;         // "#RRGGBB"
    smallIcon?: string;           // mipmap/drawable name, without prefix
    channelId?: string;           // default: "active_progress"
    ongoing?: boolean;            // default: true
    indeterminate?: boolean;      // default: false
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

export interface ActiveProgressPlugin {
    start(options: StartOptions): Promise<void>;
    update(options: UpdateOptions): Promise<void>;
    stop(options: StopOptions): Promise<void>;
}
