# V2X.tools API Migration Guide

## 🚨 Deprecated Endpoints

The following endpoints are **deprecated** and will be removed in future versions:

### `/uper2json` - DEPRECATED ⚠️
- **Deprecated since:** v1.8 (August 2025)
- **Will be removed:** v2.0 (June 2025)  
- **Sunset date:** 2025-06-01

### Migration Required

#### Old (Deprecated):
```bash
POST /uper2json
Content-Type: text/plain

020000000000000000
```

#### New (Recommended):
```bash
POST /api/v2x/uper/json
Content-Type: text/plain

020000000000000000
```

## 🔄 Migration Steps

### 1. Update Your Client Code

**Before (deprecated):**
```bash
curl -X POST \
  -H "Content-Type: text/plain" \
  -d "020000000000000000" \
  http://v2x.tools/uper2json
```

**After (recommended):**
```bash
curl -X POST \
  -H "Content-Type: text/plain" \
  -d "020000000000000000" \
  http://v2x.tools/api/v2x/uper/json
```

### 2. Benefits of New API

- **Flexible formats:** Support for multiple input/output formats
- **RESTful design:** Cleaner URL structure  
- **Better documentation:** OpenAPI 3.0 specification
- **Future-proof:** Will receive new features and improvements

### 3. Available Formats

The new API supports these format combinations:

| From Format | To Format | Endpoint |
|-------------|-----------|----------|
| UPER | JSON | `/api/v2x/uper/json` |
| UPER | XML | `/api/v2x/uper/xml` |
| JSON | UPER | `/api/v2x/json/uper` |
| XML | JSON | `/api/v2x/xml/json` |

## 📅 Deprecation Timeline

| Date | Action |
|------|--------|
| **August 2025** | Endpoints marked as deprecated (current) |
| **December 2025** | Deprecation warnings added to responses |
| **March 2025** | Final migration reminder notifications |
| **June 2025** | Legacy endpoints removed |

## 🔍 How to Detect Deprecated Usage

### HTTP Headers
Deprecated endpoints now return these headers:
```
X-API-Deprecated: true
X-API-Deprecated-Since: v1.8  
X-API-Sunset: 2025-06-01
X-API-Migration: Use POST /api/v2x/uper/json instead
```

### Server Logs
Check server logs for deprecation warnings:
```
⚠️ DEPRECATED ENDPOINT USED: /uper2json from 192.168.1.1
```

## 🆘 Support

If you need help migrating:

- **Documentation:** Visit `/swagger-ui` for interactive API docs
- **Issues:** Report migration issues on our support channels
- **Contact:** support@v2x.tools

## 📊 Testing Your Migration

### Test Both Endpoints
1. Test your current integration with `/uper2json`
2. Test the same payload with `/api/v2x/uper/json` 
3. Verify identical responses
4. Update your code to use the new endpoint

### Validation Script
```bash
#!/bin/bash
PAYLOAD="020000000000000000"
HOST="http://v2x.tools"

echo "Testing legacy endpoint..."
LEGACY=$(curl -s -X POST -H "Content-Type: text/plain" -d "$PAYLOAD" "$HOST/uper2json")

echo "Testing new endpoint..."
NEW=$(curl -s -X POST -H "Content-Type: text/plain" -d "$PAYLOAD" "$HOST/api/v2x/uper/json")

if [ "$LEGACY" = "$NEW" ]; then
    echo "✅ Migration validated - responses match"
else
    echo "❌ Migration issue - responses differ"
fi
```

---

**Need immediate assistance?** Contact our support team before the sunset date.