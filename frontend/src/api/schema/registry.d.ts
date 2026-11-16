// Sinh từ registry.json bằng `npm run api:sync`. Không sửa tay.

export interface paths {
    "/activity": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["list_9"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/code-rules": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["list_8"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/code-rules/{target}/{typeId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put: operations["put"];
        post?: never;
        delete: operations["delete_7"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/codes/backfill": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["backfill"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/codes/status": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["status"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/field-defs": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["list_6"];
        put?: never;
        post: operations["create_7"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/field-defs/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["get_7"];
        put: operations["update_7"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/field-defs/{id}/archive": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["archive_2"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/field-defs/{id}/unarchive": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["unarchive_2"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/item-categories": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["list_5"];
        put?: never;
        post: operations["create_6"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/item-categories/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["get_6"];
        put: operations["update_6"];
        post?: never;
        delete: operations["delete_6"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/item-categories/{id}/archive": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["archive_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/item-categories/{id}/unarchive": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["unarchive_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/items": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["list_4"];
        put?: never;
        post: operations["create_5"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/items/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["get_5"];
        put: operations["update_5"];
        post?: never;
        delete: operations["delete_5"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/items/{id}/move": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["move_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/items/{id}/owner": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["changeOwner"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/items/{id}/put-back": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["putBack"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/items/{id}/reissue-code": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["reissueCode_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/items/{id}/remove": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["remove"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/items/{id}/replace": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["replace"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/items/{id}/restore": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["restore_2"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/items/{id}/versions": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["versions_2"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/items/{id}/versions/{versionNo}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["version_2"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/items/{id}/versions/{versionNo}/restore": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["restoreVersion_2"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/items/search": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["search_2"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/occupancies": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["list_3"];
        put?: never;
        post: operations["create_4"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/occupancies/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["get_4"];
        put: operations["update_4"];
        post?: never;
        delete: operations["delete_4"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/occupancies/{id}/end": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["end_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/ownerships": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["list_2"];
        put?: never;
        post: operations["create_3"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/ownerships/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["get_3"];
        put: operations["update_3"];
        post?: never;
        delete: operations["delete_3"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/ownerships/{id}/end": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["end"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/ownerships/{id}/transfer": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["transfer"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/people": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["list_1"];
        put?: never;
        post: operations["create_2"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/people/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["get_2"];
        put: operations["update_2"];
        post?: never;
        delete: operations["delete_2"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/people/{id}/restore": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["restore_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/people/{id}/versions": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["versions_1"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/people/{id}/versions/{versionNo}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["version_1"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/people/{id}/versions/{versionNo}/restore": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["restoreVersion_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/people/search": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["search_1"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/place-types": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["list"];
        put?: never;
        post: operations["create_1"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/place-types/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["get_1"];
        put: operations["update_1"];
        post?: never;
        delete: operations["delete_1"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/place-types/{id}/archive": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["archive"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/place-types/{id}/unarchive": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["unarchive"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/places": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["topLevel"];
        put?: never;
        post: operations["create"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/places/{id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["get"];
        put: operations["update"];
        post?: never;
        delete: operations["delete"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/places/{id}/address": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put: operations["setAddress"];
        post?: never;
        delete: operations["removeAddress"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/places/{id}/children": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["children"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/places/{id}/move": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["move"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/places/{id}/reissue-code": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["reissueCode"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/places/{id}/restore": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["restore"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/places/{id}/subtree": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["subtree"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/places/{id}/versions": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["versions"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/places/{id}/versions/{versionNo}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["version"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/places/{id}/versions/{versionNo}/restore": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["restoreVersion"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/places/by-area": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["byArea"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/places/by-type/{typeId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["byType"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/places/search": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["search"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/setup": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post: operations["setup"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/trash": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get: operations["list_7"];
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
        ActivityResponse: {
            entityType?: string;
            entityId?: string;
            /** Format: int32 */
            versionNo?: number;
            /** @enum {string} */
            action?: "CREATE" | "UPDATE" | "DELETE" | "RESTORE";
            changedFields?: string[];
            changedBy?: string;
            /** Format: date-time */
            changedAt?: string;
            note?: string;
            entityName?: string;
        };
        AddressRequest: {
            countryCode?: string;
            provinceCode?: string;
            provinceName?: string;
            districtCode?: string;
            districtName?: string;
            wardCode?: string;
            wardName?: string;
            street?: string;
            fullAddress: string;
            latitude?: number;
            longitude?: number;
        };
        AddressResponse: {
            countryCode?: string;
            provinceCode?: string;
            provinceName?: string;
            districtCode?: string;
            districtName?: string;
            wardCode?: string;
            wardName?: string;
            street?: string;
            fullAddress?: string;
            latitude?: number;
            longitude?: number;
            geohash?: string;
        };
        AddressSnapshot: {
            countryCode?: string;
            provinceCode?: string;
            provinceName?: string;
            districtCode?: string;
            districtName?: string;
            wardCode?: string;
            wardName?: string;
            street?: string;
            fullAddress?: string;
            latitude?: number;
            longitude?: number;
            geohash?: string;
        };
        CodeBackfillResponse: {
            /** Format: int32 */
            placesAssigned?: number;
            /** Format: int32 */
            itemsAssigned?: number;
            /** Format: int32 */
            placesRemaining?: number;
            /** Format: int32 */
            itemsRemaining?: number;
        };
        CodeReissueRequest: {
            /** Format: int32 */
            version: number;
            note?: string;
        };
        CodeRuleRequest: {
            prefix: string;
            /** Format: int32 */
            digits: number;
            yearly?: boolean;
        };
        CodeRuleResponse: {
            /** @enum {string} */
            target?: "PLACE" | "ITEM";
            typeId?: string;
            prefix?: string;
            /** Format: int32 */
            digits?: number;
            yearly?: boolean;
            builtIn?: boolean;
        };
        CodeStatusResponse: {
            /** Format: int32 */
            placesWithoutCode?: number;
            /** Format: int32 */
            itemsWithoutCode?: number;
        };
        CursorPageActivityResponse: {
            items?: components["schemas"]["ActivityResponse"][];
            nextCursor?: string;
        };
        CursorPageItemResponse: {
            items?: components["schemas"]["ItemResponse"][];
            nextCursor?: string;
        };
        CursorPageItemVersionSummaryResponse: {
            items?: components["schemas"]["ItemVersionSummaryResponse"][];
            nextCursor?: string;
        };
        CursorPageOccupancyResponse: {
            items?: components["schemas"]["OccupancyResponse"][];
            nextCursor?: string;
        };
        CursorPageOwnershipResponse: {
            items?: components["schemas"]["OwnershipResponse"][];
            nextCursor?: string;
        };
        CursorPagePersonResponse: {
            items?: components["schemas"]["PersonResponse"][];
            nextCursor?: string;
        };
        CursorPagePersonVersionSummaryResponse: {
            items?: components["schemas"]["PersonVersionSummaryResponse"][];
            nextCursor?: string;
        };
        CursorPagePlaceResponse: {
            items?: components["schemas"]["PlaceResponse"][];
            nextCursor?: string;
        };
        CursorPagePlaceSearchResponse: {
            items?: components["schemas"]["PlaceSearchResponse"][];
            nextCursor?: string;
        };
        CursorPagePlaceVersionSummaryResponse: {
            items?: components["schemas"]["PlaceVersionSummaryResponse"][];
            nextCursor?: string;
        };
        CursorPageTrashResponse: {
            items?: components["schemas"]["TrashResponse"][];
            nextCursor?: string;
        };
        EffectiveAddressResponse: {
            fromPlaceId?: string;
            address?: components["schemas"]["AddressResponse"];
        };
        FieldDefCreateRequest: {
            /** @enum {string} */
            target: "PLACE" | "PERSON" | "ITEM";
            typeId?: string;
            fieldKey: string;
            label: string;
            /** @enum {string} */
            dataType: "TEXT" | "NUMBER" | "BOOLEAN" | "DATE" | "SELECT";
            options?: string[];
            required?: boolean;
            /** Format: int32 */
            sortOrder?: number;
        };
        FieldDefResponse: {
            id?: string;
            /** @enum {string} */
            target?: "PLACE" | "PERSON" | "ITEM";
            typeId?: string;
            fieldKey?: string;
            label?: string;
            /** @enum {string} */
            dataType?: "TEXT" | "NUMBER" | "BOOLEAN" | "DATE" | "SELECT";
            options?: string[];
            required?: boolean;
            /** Format: int32 */
            sortOrder?: number;
            /** @enum {string} */
            status?: "ACTIVE" | "ARCHIVED";
            /** Format: date-time */
            createdAt?: string;
            /** Format: date-time */
            updatedAt?: string;
        };
        FieldDefUpdateRequest: {
            label: string;
            options?: string[];
            required?: boolean;
            /** Format: int32 */
            sortOrder?: number;
        };
        ItemCategoryRequest: {
            name: string;
            /** Format: int32 */
            sortOrder?: number;
        };
        ItemCategoryResponse: {
            id?: string;
            name?: string;
            /** Format: int32 */
            sortOrder?: number;
            /** @enum {string} */
            status?: "ACTIVE" | "ARCHIVED";
            /** Format: date-time */
            createdAt?: string;
            /** Format: date-time */
            updatedAt?: string;
        };
        ItemDetailResponse: {
            id?: string;
            placeId?: string;
            placeName?: string;
            categoryId?: string;
            categoryName?: string;
            ownerPersonId?: string;
            ownerName?: string;
            code?: string;
            name?: string;
            /** Format: int32 */
            quantity?: number;
            unit?: string;
            /** @enum {string} */
            status?: "IN_USE" | "BROKEN" | "STORED" | "REMOVED";
            /** Format: date */
            placedAt?: string;
            /** Format: date */
            removedAt?: string;
            replacedByItemId?: string;
            description?: string;
            attributes?: {
                [key: string]: unknown;
            };
            coverFileId?: string;
            /** Format: date-time */
            createdAt?: string;
            createdBy?: string;
            /** Format: date-time */
            updatedAt?: string;
            updatedBy?: string;
            /** Format: int32 */
            version?: number;
            /** Format: date-time */
            deletedAt?: string;
        };
        ItemMoveRequest: {
            /** Format: int32 */
            version: number;
            placeId: string;
            note?: string;
        };
        ItemOwnerRequest: {
            /** Format: int32 */
            version: number;
            ownerPersonId?: string;
            note?: string;
        };
        ItemRemoveRequest: {
            /** Format: int32 */
            version: number;
            /** Format: date */
            removedAt?: string;
            note?: string;
        };
        ItemReplaceRequest: {
            /** Format: int32 */
            version: number;
            /** Format: date */
            removedAt?: string;
            replacement: components["schemas"]["ItemRequest"];
        };
        ItemReplaceResponse: {
            removed?: components["schemas"]["ItemDetailResponse"];
            replacement?: components["schemas"]["ItemDetailResponse"];
        };
        ItemRequest: {
            placeId?: string;
            ownerPersonId?: string;
            categoryId?: string;
            name: string;
            /** Format: int32 */
            quantity?: number;
            unit?: string;
            /** @enum {string} */
            status?: "IN_USE" | "BROKEN" | "STORED" | "REMOVED";
            /** Format: date */
            placedAt?: string;
            description?: string;
            attributes?: {
                [key: string]: unknown;
            };
            coverFileId?: string;
            note?: string;
        };
        ItemResponse: {
            id?: string;
            placeId?: string;
            placeName?: string;
            categoryId?: string;
            categoryName?: string;
            ownerPersonId?: string;
            ownerName?: string;
            code?: string;
            name?: string;
            /** Format: int32 */
            quantity?: number;
            unit?: string;
            /** @enum {string} */
            status?: "IN_USE" | "BROKEN" | "STORED" | "REMOVED";
            /** Format: date */
            removedAt?: string;
            /** Format: int32 */
            version?: number;
        };
        ItemSnapshot: {
            id?: string;
            placeId?: string;
            categoryId?: string;
            code?: string;
            name?: string;
            /** Format: int32 */
            quantity?: number;
            unit?: string;
            /** @enum {string} */
            status?: "IN_USE" | "BROKEN" | "STORED" | "REMOVED";
            /** Format: date */
            placedAt?: string;
            /** Format: date */
            removedAt?: string;
            replacedByItemId?: string;
            ownerPersonId?: string;
            description?: string;
            attributes?: {
                [key: string]: unknown;
            };
            coverFileId?: string;
            /** Format: date-time */
            deletedAt?: string;
        };
        ItemUpdateRequest: {
            /** Format: int32 */
            version: number;
            categoryId?: string;
            name: string;
            /** Format: int32 */
            quantity?: number;
            unit?: string;
            /** @enum {string} */
            status: "IN_USE" | "BROKEN" | "STORED" | "REMOVED";
            /** Format: date */
            placedAt?: string;
            description?: string;
            attributes?: {
                [key: string]: unknown;
            };
            coverFileId?: string;
            note?: string;
        };
        ItemVersionRequest: {
            /** Format: int32 */
            version: number;
            note?: string;
        };
        ItemVersionResponse: {
            /** Format: int32 */
            versionNo?: number;
            /** @enum {string} */
            action?: "CREATE" | "UPDATE" | "DELETE" | "RESTORE";
            snapshot?: components["schemas"]["ItemSnapshot"];
            changedFields?: string[];
            changedBy?: string;
            /** Format: date-time */
            changedAt?: string;
            note?: string;
        };
        ItemVersionSummaryResponse: {
            /** Format: int32 */
            versionNo?: number;
            /** @enum {string} */
            action?: "CREATE" | "UPDATE" | "DELETE" | "RESTORE";
            changedFields?: string[];
            changedBy?: string;
            /** Format: date-time */
            changedAt?: string;
            note?: string;
        };
        OccupancyCreateRequest: {
            placeId: string;
            personId: string;
            /** @enum {string} */
            relation: "OWNER" | "RESIDENT" | "GUEST" | "STAFF" | "OTHER";
            /** Format: date */
            startDate: string;
            /** Format: date */
            endDate?: string;
            note?: string;
        };
        OccupancyEndRequest: {
            /** Format: int32 */
            version: number;
            /** Format: date */
            endDate?: string;
        };
        OccupancyResponse: {
            id?: string;
            placeId?: string;
            placeName?: string;
            personId?: string;
            personFullName?: string;
            /** @enum {string} */
            relation?: "OWNER" | "RESIDENT" | "GUEST" | "STAFF" | "OTHER";
            /** Format: date */
            startDate?: string;
            /** Format: date */
            endDate?: string;
            /** @enum {string} */
            status?: "CURRENT" | "UPCOMING" | "PAST";
            note?: string;
            /** Format: date-time */
            createdAt?: string;
            createdBy?: string;
            /** Format: date-time */
            updatedAt?: string;
            updatedBy?: string;
            /** Format: int32 */
            version?: number;
        };
        OccupancyUpdateRequest: {
            /** Format: int32 */
            version: number;
            /** @enum {string} */
            relation: "OWNER" | "RESIDENT" | "GUEST" | "STAFF" | "OTHER";
            /** Format: date */
            startDate: string;
            /** Format: date */
            endDate?: string;
            note?: string;
        };
        OwnershipCreateRequest: {
            placeId: string;
            personId: string;
            sharePercent?: number;
            /** Format: date */
            startDate: string;
            /** Format: date */
            endDate?: string;
            documentNo?: string;
            note?: string;
        };
        OwnershipEndRequest: {
            /** Format: int32 */
            version: number;
            /** Format: date */
            endDate?: string;
        };
        OwnershipResponse: {
            id?: string;
            placeId?: string;
            placeName?: string;
            personId?: string;
            personFullName?: string;
            sharePercent?: number;
            /** Format: date */
            startDate?: string;
            /** Format: date */
            endDate?: string;
            /** @enum {string} */
            status?: "CURRENT" | "UPCOMING" | "PAST";
            documentNo?: string;
            note?: string;
            /** Format: date-time */
            createdAt?: string;
            createdBy?: string;
            /** Format: date-time */
            updatedAt?: string;
            updatedBy?: string;
            /** Format: int32 */
            version?: number;
        };
        OwnershipTransferRequest: {
            /** Format: int32 */
            version: number;
            toPersonId: string;
            /** Format: date */
            transferDate?: string;
            documentNo?: string;
            note?: string;
        };
        OwnershipTransferResponse: {
            ended?: components["schemas"]["OwnershipResponse"];
            started?: components["schemas"]["OwnershipResponse"];
        };
        OwnershipUpdateRequest: {
            /** Format: int32 */
            version: number;
            sharePercent?: number;
            /** Format: date */
            startDate: string;
            /** Format: date */
            endDate?: string;
            documentNo?: string;
            note?: string;
        };
        PersonDetailResponse: {
            id?: string;
            fullName?: string;
            phone?: string;
            email?: string;
            idType?: string;
            idNumber?: string;
            /** Format: date */
            dateOfBirth?: string;
            /** @enum {string} */
            gender?: "MALE" | "FEMALE" | "OTHER";
            note?: string;
            attributes?: {
                [key: string]: unknown;
            };
            avatarFileId?: string;
            /** Format: date-time */
            createdAt?: string;
            createdBy?: string;
            /** Format: date-time */
            updatedAt?: string;
            updatedBy?: string;
            /** Format: int32 */
            version?: number;
            /** Format: date-time */
            deletedAt?: string;
            currentOccupancies?: components["schemas"]["OccupancyResponse"][];
        };
        PersonRequest: {
            fullName: string;
            phone?: string;
            /** Format: email */
            email?: string;
            idType?: string;
            idNumber?: string;
            /** Format: date */
            dateOfBirth?: string;
            /** @enum {string} */
            gender?: "MALE" | "FEMALE" | "OTHER";
            note?: string;
            attributes?: {
                [key: string]: unknown;
            };
            avatarFileId?: string;
            versionNote?: string;
        };
        PersonResponse: {
            id?: string;
            fullName?: string;
            phone?: string;
            idType?: string;
            idNumber?: string;
            /** @enum {string} */
            gender?: "MALE" | "FEMALE" | "OTHER";
            /** Format: int32 */
            version?: number;
        };
        PersonRestoreRequest: {
            /** Format: int32 */
            version: number;
            note?: string;
        };
        PersonSnapshot: {
            id?: string;
            fullName?: string;
            phone?: string;
            email?: string;
            idType?: string;
            idNumber?: string;
            /** Format: date */
            dateOfBirth?: string;
            /** @enum {string} */
            gender?: "MALE" | "FEMALE" | "OTHER";
            note?: string;
            attributes?: {
                [key: string]: unknown;
            };
            avatarFileId?: string;
            /** Format: date-time */
            deletedAt?: string;
        };
        PersonUpdateRequest: {
            /** Format: int32 */
            version: number;
            fullName: string;
            phone?: string;
            /** Format: email */
            email?: string;
            idType?: string;
            idNumber?: string;
            /** Format: date */
            dateOfBirth?: string;
            /** @enum {string} */
            gender?: "MALE" | "FEMALE" | "OTHER";
            note?: string;
            attributes?: {
                [key: string]: unknown;
            };
            avatarFileId?: string;
            versionNote?: string;
        };
        PersonVersionResponse: {
            /** Format: int32 */
            versionNo?: number;
            /** @enum {string} */
            action?: "CREATE" | "UPDATE" | "DELETE" | "RESTORE";
            snapshot?: components["schemas"]["PersonSnapshot"];
            changedFields?: string[];
            changedBy?: string;
            /** Format: date-time */
            changedAt?: string;
            note?: string;
        };
        PersonVersionRestoreRequest: {
            /** Format: int32 */
            version: number;
            note?: string;
        };
        PersonVersionSummaryResponse: {
            /** Format: int32 */
            versionNo?: number;
            /** @enum {string} */
            action?: "CREATE" | "UPDATE" | "DELETE" | "RESTORE";
            changedFields?: string[];
            changedBy?: string;
            /** Format: date-time */
            changedAt?: string;
            note?: string;
        };
        PlaceAddressRequest: {
            /** Format: int32 */
            version: number;
            address: components["schemas"]["AddressRequest"];
            note?: string;
        };
        PlaceCreateRequest: {
            parentId?: string;
            placeTypeId: string;
            name: string;
            description?: string;
            /** @enum {string} */
            status?: "ACTIVE" | "INACTIVE";
            attributes?: {
                [key: string]: unknown;
            };
            coverFileId?: string;
            address?: components["schemas"]["AddressRequest"];
            note?: string;
        };
        PlaceDetailResponse: {
            id?: string;
            parentId?: string;
            placeTypeId?: string;
            path?: string;
            /** Format: int32 */
            depth?: number;
            code?: string;
            name?: string;
            description?: string;
            /** @enum {string} */
            status?: "ACTIVE" | "INACTIVE";
            attributes?: {
                [key: string]: unknown;
            };
            coverFileId?: string;
            /** Format: date-time */
            createdAt?: string;
            createdBy?: string;
            /** Format: date-time */
            updatedAt?: string;
            updatedBy?: string;
            /** Format: int32 */
            version?: number;
            /** Format: date-time */
            deletedAt?: string;
            ancestors?: components["schemas"]["PlaceRef"][];
            address?: components["schemas"]["AddressResponse"];
            effectiveAddress?: components["schemas"]["EffectiveAddressResponse"];
        };
        PlaceMoveRequest: {
            /** Format: int32 */
            version: number;
            parentId?: string;
            note?: string;
        };
        PlaceRef: {
            id?: string;
            name?: string;
        };
        PlaceResponse: {
            id?: string;
            parentId?: string;
            placeTypeId?: string;
            code?: string;
            name?: string;
            /** @enum {string} */
            status?: "ACTIVE" | "INACTIVE";
            /** Format: int32 */
            depth?: number;
            coverFileId?: string;
            /** Format: int32 */
            version?: number;
            hasChildren?: boolean;
        };
        PlaceRestoreRequest: {
            /** Format: int32 */
            version: number;
            note?: string;
        };
        PlaceSearchResponse: {
            id?: string;
            parentId?: string;
            placeTypeId?: string;
            code?: string;
            name?: string;
            /** @enum {string} */
            status?: "ACTIVE" | "INACTIVE";
            /** Format: int32 */
            depth?: number;
            coverFileId?: string;
            /** Format: int32 */
            version?: number;
            hasChildren?: boolean;
            ancestors?: components["schemas"]["PlaceRef"][];
        };
        PlaceSnapshot: {
            id?: string;
            parentId?: string;
            placeTypeId?: string;
            path?: string;
            /** Format: int32 */
            depth?: number;
            code?: string;
            name?: string;
            description?: string;
            /** @enum {string} */
            status?: "ACTIVE" | "INACTIVE";
            attributes?: {
                [key: string]: unknown;
            };
            coverFileId?: string;
            /** Format: date-time */
            deletedAt?: string;
            address?: components["schemas"]["AddressSnapshot"];
        };
        PlaceTypeCreateRequest: {
            code: string;
            name: string;
            description?: string;
            /** Format: int32 */
            sortOrder?: number;
        };
        PlaceTypeResponse: {
            id?: string;
            code?: string;
            name?: string;
            description?: string;
            /** Format: int32 */
            sortOrder?: number;
            /** @enum {string} */
            status?: "ACTIVE" | "ARCHIVED";
            /** Format: date-time */
            createdAt?: string;
            /** Format: date-time */
            updatedAt?: string;
        };
        PlaceTypeUpdateRequest: {
            name: string;
            description?: string;
            /** Format: int32 */
            sortOrder?: number;
        };
        PlaceUpdateRequest: {
            /** Format: int32 */
            version: number;
            placeTypeId: string;
            name: string;
            description?: string;
            /** @enum {string} */
            status: "ACTIVE" | "INACTIVE";
            attributes?: {
                [key: string]: unknown;
            };
            coverFileId?: string;
            note?: string;
        };
        PlaceVersionResponse: {
            /** Format: int32 */
            versionNo?: number;
            /** @enum {string} */
            action?: "CREATE" | "UPDATE" | "DELETE" | "RESTORE";
            snapshot?: components["schemas"]["PlaceSnapshot"];
            changedFields?: string[];
            changedBy?: string;
            /** Format: date-time */
            changedAt?: string;
            note?: string;
        };
        PlaceVersionRestoreRequest: {
            /** Format: int32 */
            version: number;
            note?: string;
        };
        PlaceVersionSummaryResponse: {
            /** Format: int32 */
            versionNo?: number;
            /** @enum {string} */
            action?: "CREATE" | "UPDATE" | "DELETE" | "RESTORE";
            changedFields?: string[];
            changedBy?: string;
            /** Format: date-time */
            changedAt?: string;
            note?: string;
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
        SetupResponse: {
            created?: boolean;
        };
        TrashResponse: {
            /** @enum {string} */
            entityType?: "PLACE" | "PERSON" | "ITEM";
            id?: string;
            name?: string;
            code?: string;
            context?: string;
            /** Format: date-time */
            deletedAt?: string;
            deletedBy?: string;
            /** Format: int32 */
            version?: number;
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
    list_9: {
        parameters: {
            query?: {
                entityType?: string;
                /** @description nextCursor of the previous page; omit for the first page */
                cursor?: string;
                /** @description Maximum number of items in the page */
                limit?: number;
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
                    "*/*": components["schemas"]["CursorPageActivityResponse"];
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
    list_8: {
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
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["CodeRuleResponse"][];
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
    put: {
        parameters: {
            query?: never;
            header: {
                /** @description Id of the workspace the call works in */
                "X-Workspace-Id": string;
                /** @description Id of the calling user */
                "X-User-Id": string;
            };
            path: {
                target: "PLACE" | "ITEM";
                typeId: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CodeRuleRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["CodeRuleResponse"];
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
    delete_7: {
        parameters: {
            query?: never;
            header: {
                /** @description Id of the workspace the call works in */
                "X-Workspace-Id": string;
                /** @description Id of the calling user */
                "X-User-Id": string;
            };
            path: {
                target: "PLACE" | "ITEM";
                typeId: string;
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
    backfill: {
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
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["CodeBackfillResponse"];
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
    status: {
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
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["CodeStatusResponse"];
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
    list_6: {
        parameters: {
            query: {
                target: "PLACE" | "PERSON" | "ITEM";
                typeId?: string;
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
                    "*/*": components["schemas"]["FieldDefResponse"][];
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
    create_7: {
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
                "application/json": components["schemas"]["FieldDefCreateRequest"];
            };
        };
        responses: {
            /** @description Created */
            201: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["FieldDefResponse"];
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
    get_7: {
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
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["FieldDefResponse"];
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
    update_7: {
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
                "application/json": components["schemas"]["FieldDefUpdateRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["FieldDefResponse"];
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
    archive_2: {
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
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["FieldDefResponse"];
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
    unarchive_2: {
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
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["FieldDefResponse"];
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
    list_5: {
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
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ItemCategoryResponse"][];
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
    create_6: {
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
                "application/json": components["schemas"]["ItemCategoryRequest"];
            };
        };
        responses: {
            /** @description Created */
            201: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ItemCategoryResponse"];
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
    get_6: {
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
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ItemCategoryResponse"];
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
    update_6: {
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
                "application/json": components["schemas"]["ItemCategoryRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ItemCategoryResponse"];
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
    delete_6: {
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
    archive_1: {
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
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ItemCategoryResponse"];
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
    unarchive_1: {
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
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ItemCategoryResponse"];
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
    list_4: {
        parameters: {
            query?: {
                placeId?: string;
                removed?: boolean;
                subtreeOf?: string;
                categoryId?: string;
                ownerId?: string;
                status?: "IN_USE" | "BROKEN" | "STORED" | "REMOVED";
                /** @description nextCursor of the previous page; omit for the first page */
                cursor?: string;
                /** @description Maximum number of items in the page */
                limit?: number;
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
                    "*/*": components["schemas"]["CursorPageItemResponse"];
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
    create_5: {
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
                "application/json": components["schemas"]["ItemRequest"];
            };
        };
        responses: {
            /** @description Created */
            201: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ItemDetailResponse"];
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
    get_5: {
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
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ItemDetailResponse"];
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
    update_5: {
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
                "application/json": components["schemas"]["ItemUpdateRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ItemDetailResponse"];
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
    delete_5: {
        parameters: {
            query: {
                version: number;
                note?: string;
            };
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
    move_1: {
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
                "application/json": components["schemas"]["ItemMoveRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ItemDetailResponse"];
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
    changeOwner: {
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
                "application/json": components["schemas"]["ItemOwnerRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ItemDetailResponse"];
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
    putBack: {
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
                "application/json": components["schemas"]["ItemVersionRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ItemDetailResponse"];
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
    reissueCode_1: {
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
                "application/json": components["schemas"]["CodeReissueRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ItemDetailResponse"];
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
        requestBody: {
            content: {
                "application/json": components["schemas"]["ItemRemoveRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ItemDetailResponse"];
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
    replace: {
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
                "application/json": components["schemas"]["ItemReplaceRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ItemReplaceResponse"];
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
    restore_2: {
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
                "application/json": components["schemas"]["ItemVersionRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ItemDetailResponse"];
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
    versions_2: {
        parameters: {
            query?: {
                /** @description nextCursor of the previous page; omit for the first page */
                cursor?: string;
                /** @description Maximum number of items in the page */
                limit?: number;
            };
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
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["CursorPageItemVersionSummaryResponse"];
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
    version_2: {
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
                versionNo: number;
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
                    "*/*": components["schemas"]["ItemVersionResponse"];
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
    restoreVersion_2: {
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
                versionNo: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ItemVersionRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["ItemDetailResponse"];
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
    search_2: {
        parameters: {
            query?: {
                name?: string;
                code?: string;
                /** @description nextCursor of the previous page; omit for the first page */
                cursor?: string;
                /** @description Maximum number of items in the page */
                limit?: number;
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
                    "*/*": components["schemas"]["CursorPageItemResponse"];
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
    list_3: {
        parameters: {
            query?: {
                placeId?: string;
                status?: "CURRENT" | "UPCOMING" | "PAST";
                subtreeOf?: string;
                personId?: string;
                /** @description nextCursor of the previous page; omit for the first page */
                cursor?: string;
                /** @description Maximum number of items in the page */
                limit?: number;
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
                    "*/*": components["schemas"]["CursorPageOccupancyResponse"];
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
    create_4: {
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
                "application/json": components["schemas"]["OccupancyCreateRequest"];
            };
        };
        responses: {
            /** @description Created */
            201: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["OccupancyResponse"];
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
    get_4: {
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
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["OccupancyResponse"];
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
    update_4: {
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
                "application/json": components["schemas"]["OccupancyUpdateRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["OccupancyResponse"];
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
    delete_4: {
        parameters: {
            query: {
                version: number;
            };
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
    end_1: {
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
                "application/json": components["schemas"]["OccupancyEndRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["OccupancyResponse"];
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
    list_2: {
        parameters: {
            query?: {
                placeId?: string;
                status?: "CURRENT" | "UPCOMING" | "PAST";
                subtreeOf?: string;
                personId?: string;
                /** @description nextCursor of the previous page; omit for the first page */
                cursor?: string;
                /** @description Maximum number of items in the page */
                limit?: number;
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
                    "*/*": components["schemas"]["CursorPageOwnershipResponse"];
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
    create_3: {
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
                "application/json": components["schemas"]["OwnershipCreateRequest"];
            };
        };
        responses: {
            /** @description Created */
            201: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["OwnershipResponse"];
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
    get_3: {
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
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["OwnershipResponse"];
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
    update_3: {
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
                "application/json": components["schemas"]["OwnershipUpdateRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["OwnershipResponse"];
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
    delete_3: {
        parameters: {
            query: {
                version: number;
            };
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
    end: {
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
                "application/json": components["schemas"]["OwnershipEndRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["OwnershipResponse"];
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
    transfer: {
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
                "application/json": components["schemas"]["OwnershipTransferRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["OwnershipTransferResponse"];
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
    list_1: {
        parameters: {
            query?: {
                /** @description nextCursor of the previous page; omit for the first page */
                cursor?: string;
                /** @description Maximum number of items in the page */
                limit?: number;
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
                    "*/*": components["schemas"]["CursorPagePersonResponse"];
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
    create_2: {
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
                "application/json": components["schemas"]["PersonRequest"];
            };
        };
        responses: {
            /** @description Created */
            201: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["PersonDetailResponse"];
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
    get_2: {
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
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["PersonDetailResponse"];
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
    update_2: {
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
                "application/json": components["schemas"]["PersonUpdateRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["PersonDetailResponse"];
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
    delete_2: {
        parameters: {
            query: {
                version: number;
                note?: string;
            };
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
    restore_1: {
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
                "application/json": components["schemas"]["PersonRestoreRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["PersonDetailResponse"];
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
    versions_1: {
        parameters: {
            query?: {
                /** @description nextCursor of the previous page; omit for the first page */
                cursor?: string;
                /** @description Maximum number of items in the page */
                limit?: number;
            };
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
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["CursorPagePersonVersionSummaryResponse"];
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
    version_1: {
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
                versionNo: number;
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
                    "*/*": components["schemas"]["PersonVersionResponse"];
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
    restoreVersion_1: {
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
                versionNo: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["PersonVersionRestoreRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["PersonDetailResponse"];
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
    search_1: {
        parameters: {
            query?: {
                name?: string;
                phone?: string;
                idNumber?: string;
                /** @description nextCursor of the previous page; omit for the first page */
                cursor?: string;
                /** @description Maximum number of items in the page */
                limit?: number;
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
                    "*/*": components["schemas"]["CursorPagePersonResponse"];
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
    list: {
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
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["PlaceTypeResponse"][];
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
    create_1: {
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
                "application/json": components["schemas"]["PlaceTypeCreateRequest"];
            };
        };
        responses: {
            /** @description Created */
            201: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["PlaceTypeResponse"];
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
    get_1: {
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
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["PlaceTypeResponse"];
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
    update_1: {
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
                "application/json": components["schemas"]["PlaceTypeUpdateRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["PlaceTypeResponse"];
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
    delete_1: {
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
    archive: {
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
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["PlaceTypeResponse"];
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
    unarchive: {
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
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["PlaceTypeResponse"];
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
    topLevel: {
        parameters: {
            query?: {
                /** @description nextCursor of the previous page; omit for the first page */
                cursor?: string;
                /** @description Maximum number of items in the page */
                limit?: number;
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
                    "*/*": components["schemas"]["CursorPagePlaceResponse"];
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
    create: {
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
                "application/json": components["schemas"]["PlaceCreateRequest"];
            };
        };
        responses: {
            /** @description Created */
            201: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["PlaceDetailResponse"];
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
    get: {
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
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["PlaceDetailResponse"];
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
    update: {
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
                "application/json": components["schemas"]["PlaceUpdateRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["PlaceDetailResponse"];
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
    delete: {
        parameters: {
            query: {
                version: number;
                note?: string;
            };
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
    setAddress: {
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
                "application/json": components["schemas"]["PlaceAddressRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["PlaceDetailResponse"];
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
    removeAddress: {
        parameters: {
            query: {
                version: number;
                note?: string;
            };
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
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["PlaceDetailResponse"];
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
    children: {
        parameters: {
            query?: {
                /** @description nextCursor of the previous page; omit for the first page */
                cursor?: string;
                /** @description Maximum number of items in the page */
                limit?: number;
            };
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
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["CursorPagePlaceResponse"];
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
    move: {
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
                "application/json": components["schemas"]["PlaceMoveRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["PlaceDetailResponse"];
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
    reissueCode: {
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
                "application/json": components["schemas"]["CodeReissueRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["PlaceDetailResponse"];
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
    restore: {
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
                "application/json": components["schemas"]["PlaceRestoreRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["PlaceDetailResponse"];
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
    subtree: {
        parameters: {
            query?: {
                /** @description nextCursor of the previous page; omit for the first page */
                cursor?: string;
                /** @description Maximum number of items in the page */
                limit?: number;
            };
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
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["CursorPagePlaceResponse"];
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
    versions: {
        parameters: {
            query?: {
                /** @description nextCursor of the previous page; omit for the first page */
                cursor?: string;
                /** @description Maximum number of items in the page */
                limit?: number;
            };
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
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["CursorPagePlaceVersionSummaryResponse"];
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
    version: {
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
                versionNo: number;
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
                    "*/*": components["schemas"]["PlaceVersionResponse"];
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
    restoreVersion: {
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
                versionNo: number;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["PlaceVersionRestoreRequest"];
            };
        };
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["PlaceDetailResponse"];
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
    byArea: {
        parameters: {
            query: {
                province: string;
                ward?: string;
                /** @description nextCursor of the previous page; omit for the first page */
                cursor?: string;
                /** @description Maximum number of items in the page */
                limit?: number;
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
                    "*/*": components["schemas"]["CursorPagePlaceResponse"];
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
    byType: {
        parameters: {
            query?: {
                /** @description nextCursor of the previous page; omit for the first page */
                cursor?: string;
                /** @description Maximum number of items in the page */
                limit?: number;
            };
            header: {
                /** @description Id of the workspace the call works in */
                "X-Workspace-Id": string;
                /** @description Id of the calling user */
                "X-User-Id": string;
            };
            path: {
                typeId: string;
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
                    "*/*": components["schemas"]["CursorPagePlaceResponse"];
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
    search: {
        parameters: {
            query?: {
                name?: string;
                code?: string;
                /** @description nextCursor of the previous page; omit for the first page */
                cursor?: string;
                /** @description Maximum number of items in the page */
                limit?: number;
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
                    "*/*": components["schemas"]["CursorPagePlaceSearchResponse"];
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
    setup: {
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
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "*/*": components["schemas"]["SetupResponse"];
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
    list_7: {
        parameters: {
            query: {
                entityType: "PLACE" | "PERSON" | "ITEM";
                /** @description nextCursor of the previous page; omit for the first page */
                cursor?: string;
                /** @description Maximum number of items in the page */
                limit?: number;
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
                    "*/*": components["schemas"]["CursorPageTrashResponse"];
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
