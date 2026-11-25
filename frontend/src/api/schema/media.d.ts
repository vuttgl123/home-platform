// Sinh từ media.json bằng `npm run api:sync`. Không sửa tay.

export interface paths {
    "/attachments": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["list"];
        put?: never;
        post: operations["upload"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/attachments/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put: operations["caption"];
        post?: never;
        delete: operations["remove"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/attachments/order": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put: operations["reorder"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workspaces/{workspaceId}/files/{fileId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["original"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/workspaces/{workspaceId}/files/{fileId}/thumbnail": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["thumbnail"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
}
export type webhooks = Record<string, never>;
export interface components {
    schemas: {
        AttachmentResponse: {
            id?: string;
            /** @enum {string} */
            ownerType?: "PLACE" | "PERSON" | "ITEM";
            ownerId?: string;
            fileId?: string;
            caption?: string;
            /** Format: int32 */
            sortOrder?: number;
            addedBy?: string;
            /** Format: date-time */
            addedAt?: string;
            file?: components["schemas"]["FileInfo"];
        };
        CaptionRequest: {
            caption?: string;
        };
        FileInfo: {
            originalName?: string;
            contentType?: string;
            sizeBytes?: string;
            /** Format: int32 */
            width?: number;
            /** Format: int32 */
            height?: number;
        };
        OrderRequest: {
            /** @enum {string} */
            ownerType: "PLACE" | "PERSON" | "ITEM";
            ownerId: string;
            attachmentIds: string[];
        };
        /** @description Error response (RFC 9457 Problem Details) */
        ProblemDetail: {
            /** Format: uri */
            type?: string;
            title?: string;
            /** Format: int32 */
            status?: number;
            detail?: string;
            /** Format: uri */
            instance?: string;
            /** @description Machine-readable reason, when there is one; VERSION_CONFLICT means read again */
            code?: string;
        };
    };
    responses: never;
    parameters: never;
    requestBodies: never;
    headers: never;
    pathItems: never;
}
export type $defs = Record<string, never>;
export interface operations {
    list: {
        parameters: {
            query: {
                ownerType: "PLACE" | "PERSON" | "ITEM";
                ownerId: string;
            };
            header: {
                /** @description Id of the workspace the call works in */
                "X-Workspace-Id": string;
                /** @description Id of the calling user */
                "X-User-Id": string;
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["AttachmentResponse"][];
                };
            };
            /** @description Error, described as Problem Details */
            default: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    upload: {
        parameters: {
            query: {
                ownerType: "PLACE" | "PERSON" | "ITEM";
                ownerId: string;
                caption?: string;
            };
            header: {
                /** @description Id of the workspace the call works in */
                "X-Workspace-Id": string;
                /** @description Id of the calling user */
                "X-User-Id": string;
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: {
            content: {
                "multipart/form-data": {
                    /** Format: binary */
                    file: string;
                };
            };
        };
        responses: {
            /** @description Created */
            201: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["AttachmentResponse"];
                };
            };
            /** @description Error, described as Problem Details */
            default: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    caption: {
        parameters: {
            query?: never;
            header: {
                /** @description Id of the workspace the call works in */
                "X-Workspace-Id": string;
                /** @description Id of the calling user */
                "X-User-Id": string;
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CaptionRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["AttachmentResponse"];
                };
            };
            /** @description Error, described as Problem Details */
            default: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    remove: {
        parameters: {
            query?: never;
            header: {
                /** @description Id of the workspace the call works in */
                "X-Workspace-Id": string;
                /** @description Id of the calling user */
                "X-User-Id": string;
            };
            path: {
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description No Content */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
            /** @description Error, described as Problem Details */
            default: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    reorder: {
        parameters: {
            query?: never;
            header: {
                /** @description Id of the workspace the call works in */
                "X-Workspace-Id": string;
                /** @description Id of the calling user */
                "X-User-Id": string;
            };
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["OrderRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["AttachmentResponse"][];
                };
            };
            /** @description Error, described as Problem Details */
            default: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    original: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                workspaceId: string;
                fileId: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
            /** @description Error, described as Problem Details */
            default: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
    thumbnail: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                workspaceId: string;
                fileId: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": string;
                };
            };
            /** @description Error, described as Problem Details */
            default: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/problem+json": components["schemas"]["ProblemDetail"];
                };
            };
        };
    };
}
