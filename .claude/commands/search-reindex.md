# /search-reindex

**Status**: Available after Track 5 (Advanced Search) is implemented.

Create or trigger a full reindex of an entity type into OpenSearch, or create/update an index mapping for a new entity.

## Ask me for:
1. Entity type to index (e.g., `customer`, `opportunity`, `invoice`)
2. Action: `CREATE_INDEX` (new entity), `REINDEX_ALL` (all records of existing entity), or `REINDEX_ENTITY` (single record)
3. Optional: custom field definitions to include in mapping (for new entities)

## What I'll do:

### For CREATE_INDEX:
1. Create OpenSearch index with tenant-aware name: `bocrm_{tenantId}_{entity}`
2. Define mappings for:
   - Scalar fields (name, status, dates, numbers)
   - JSONB custom fields (flattened as `cf_*` dynamic fields)
   - Calculated field values (flattened as `calc_*` fields)
3. Create migration `V{N}__add_search_index_for_entity.sql` (idempotent, includes mapping config)

### For REINDEX_ALL:
1. Call `POST /admin/search/reindex?entityType=entity` backend endpoint
2. Fetches all entities of that type (paginated) for current tenant
3. For each: builds index document with custom fields + calculated values
4. Bulk upserts to OpenSearch
5. Updates `search_index_status` table with completion timestamp

### For REINDEX_ENTITY:
1. Fetch single entity + custom fields + calculated values
2. Build index document
3. Upsert to OpenSearch

## Example index mapping (customer):
```json
{
  "mappings": {
    "properties": {
      "id": {"type": "keyword"},
      "tenantId": {"type": "keyword"},
      "name": {"type": "text"},
      "email": {"type": "keyword"},
      "status": {"type": "keyword"},
      "cf_industry": {"type": "text"},
      "cf_annual_revenue": {"type": "long"},
      "calc_lifetime_value": {"type": "double"},
      "createdAt": {"type": "date"}
    }
  }
}
```

## Notes:
- Tenant isolation: index name includes tenantId; every search query filters by tenantId
- Custom fields: denormalized from JSONB at index time for full-text search
- Workflow fields: `cf_{key}_currentIndex` stored as numeric for range queries
- Bulk reindex is async and can be monitored via job status endpoint
