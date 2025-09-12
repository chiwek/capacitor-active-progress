import { registerPlugin } from '@capacitor/core';
import type { ActiveProgressPlugin } from './definitions';

export * from './definitions';
export const ActiveProgress = registerPlugin<ActiveProgressPlugin>('ActiveProgress');
