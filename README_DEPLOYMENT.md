# Quick Deployment Guide

## 🚀 Deploy to Render in 5 Minutes

### Option 1: One-Click Deploy (Easiest)

1. Push your code to GitHub
2. Go to [Render Dashboard](https://dashboard.render.com)
3. Click **"New +"** → **"Blueprint"**
4. Connect your repository
5. Set environment variable: `BASE_URL=https://your-app.onrender.com`
6. Click **"Apply"**
7. Done! ✅

### Option 2: Manual Deploy

See [RENDER_DEPLOYMENT.md](RENDER_DEPLOYMENT.md) for detailed instructions.

---

## 📝 What Was Configured

### Files Created/Modified:
- ✅ `render.yaml` - Render deployment blueprint
- ✅ `application-prod.properties` - Production configuration
- ✅ `DatabaseConfig.java` - Automatic URL conversion for Render
- ✅ `CacheConfig.java` - In-memory cache fallback
- ✅ `HealthController.java` - Enhanced health checks
- ✅ `Dockerfile` - Container support
- ✅ `build.sh` - Build script
- ✅ `RENDER_DEPLOYMENT.md` - Complete deployment guide

### Key Features:
- 🔄 **Automatic database URL conversion** (Render format → JDBC format)
- 💾 **In-memory cache fallback** (no Redis needed on free tier)
- 🏥 **Health check endpoints** (`/api/health`, `/api/health/ready`, `/api/health/live`)
- 🔧 **Optimized JVM settings** for 512MB RAM
- 📊 **Connection pool optimization** for free tier
- 🔒 **Security ready** with configurable admin credentials

---

## ⚡ Test Locally First

Before deploying, test with production profile locally:

```bash
# Set environment variables
export SPRING_PROFILES_ACTIVE=prod
export DATABASE_URL=jdbc:postgresql://localhost:5432/urlshortener
export DB_USERNAME=postgres
export DB_PASSWORD=postgres

# Build and run
./mvnw clean package -DskipTests
java -jar target/urlshortener-0.0.1-SNAPSHOT.jar
```

Test health endpoint:
```bash
curl http://localhost:8080/api/health
```

---

## 🔧 Environment Variables Required

| Variable | Description | Example |
|----------|-------------|---------|
| `DATABASE_URL` | PostgreSQL connection URL | Auto-set by Render |
| `DB_USERNAME` | Database username | Auto-set by Render |
| `DB_PASSWORD` | Database password | Auto-set by Render |
| `BASE_URL` | Your app URL | `https://your-app.onrender.com` |
| `ADMIN_USERNAME` | Admin user | `admin` |
| `ADMIN_PASSWORD` | Admin password | `changeme123` |

---

## 📋 Post-Deployment Checklist

After deployment:

1. ✅ Check health: `https://your-app.onrender.com/api/health`
2. ✅ Test URL shortening: POST to `/api/shorten`
3. ✅ Test redirect: GET `/{shortCode}`
4. ✅ Check database: Verify tables created
5. ✅ Check logs: No errors in Render dashboard
6. ✅ Update `BASE_URL` environment variable with actual Render URL
7. ✅ Change `ADMIN_PASSWORD` from default

---

## 🆘 Troubleshooting

### App won't start?
- Check logs in Render dashboard
- Verify DATABASE_URL is set
- Ensure PostgreSQL database is running

### Slow response?
- Normal on free tier (cold starts)
- First request after 15 minutes takes ~30 seconds

### Out of memory?
- Check JVM settings in render.yaml
- Reduce connection pool size

For detailed troubleshooting, see [RENDER_DEPLOYMENT.md](RENDER_DEPLOYMENT.md)

---

**Ready to deploy!** 🎉
