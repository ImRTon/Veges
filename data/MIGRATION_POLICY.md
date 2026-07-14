# Database Migration Policy

Veges database upgrades must use explicit, reviewed Room migrations. Destructive migration
fallback is intentionally not configured. A migration is accepted only after its exported schema,
upgrade test, downgrade behavior, and rollback implications have been reviewed.

If a migration cannot preserve user data, the release is blocked rather than silently deleting the
local database.
