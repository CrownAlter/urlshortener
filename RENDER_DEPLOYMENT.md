# Render Deployment Guide - URL Shortener

This guide provides step-by-step instructions for deploying the URL Shortener application on Render's **FREE tier**.

## 🎯 Prerequisites

- A [Render account](https://render.com) (free tier)
- Git repository with your code pushed to GitHub/GitLab/Bitbucket
- Basic understanding of PostgreSQL and Spring Boot

## 📋 What's Included

This project has been configured with:

✅ **Production-ready configuration** (`application-prod.properties`)  
✅ **Render Blueprint** (`render.yaml`)  
✅ **Docker support** (`Dockerfile`)  
✅ **Database configuration** with automatic URL conversion  
✅ **Redis fallback** to in-memory cache (free tier compatible)  
✅ **Health checks** for monitoring  
✅ **Optimized JVM settings** for 512MB RAM limit  

---

## 🚀 Deployment Steps

### Method 1: Using Render Blueprint (Recommended)

1. **Connect Your Repository**
   - Go to [Render Dashboard](https://dashboard.render.com)
   - Click **"New +"** → **"Blueprint"**
   - Connect your Git repository
   - Render will automatically detect the `render.yaml` file

2. **Configure Environment Variables**
   - Render will automatically set up:
     - `DATABASE_URL` - PostgreSQL connection string
     - `DB_USERNAME` - Database username
     - `DB_PASSWORD` - Database password
   - You should manually add:
     - `BASE_URL` - Your Render app URL (e.g., `https://your-app.onrender.com`)
     - `ADMIN_USERNAME` - Admin username for secured endpoints (default: `admin`)
     - `ADMIN_PASSWORD` - Admin password (change from default!)

3. **Deploy**
   - Click **"Apply"**
   - Render will:
     - Create PostgreSQL database
     - Build your application
     - Deploy the web service
     - Run health checks

### Method 2: Manual Setup

#### Step 1: Create PostgreSQL Database

1. Go to Render Dashboard → **"New +"** → **"PostgreSQL"**
2. Configure:
   - **Name**: `urlshortener-db`
   - **Database**: `urlshortener`
   - **User**: `urlshortener_user`
   - **Plan**: Free
3. Click **"Create Database"**
4. Copy the **Internal Database URL** (starts with `postgres://`)

#### Step 2: Create Web Service

1. Go to Render Dashboard → **"New +"** → **"Web Service"**
2. Connect your Git repository
3. Configure:
   - **Name**: `urlshortener`
   - **Runtime**: `Java`
   - **Build Command**: `./mvnw clean package -DskipTests`
   - **Start Command**: `java -Dserver.port=$PORT -Dspring.profiles.active=prod -jar target/urlshortener-0.0.1-SNAPSHOT.jar`
   - **Plan**: Free

4. **Environment Variables**:
   ```
   JAVA_TOOL_OPTIONS=-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0
   SPRING_PROFILES_ACTIVE=prod
   DATABASE_URL=[Your Internal Database URL from Step 1]
   DB_USERNAME=[Database username]
   DB_PASSWORD=[Database password]
   BASE_URL=https://[your-app-name].onrender.com
   ADMIN_USERNAME=admin
   ADMIN_PASSWORD=[Choose a secure password]
   ```

5. **Advanced Settings**:
   - **Health Check Path**: `/api/health`
   - **Auto-Deploy**: Yes

6. Click **"Create Web Service"**

---

## 🔍 Verify Deployment

Once deployed, test these endpoints:

### Health Check
```bash
curl https://your-app.onrender.com/api/health
```

Expected response:
```json
{
  "status": "UP",
  "timestamp": "2024-01-15T10:30:00",
  "service": "URL Shortener",
  "database": "UP",
  "databaseType": "PostgreSQL",
  "cache": "In-Memory",
  "redisEnabled": false
}
```

### Create Short URL
```bash
curl -X POST https://your-app.onrender.com/api/shorten \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://example.com/very-long-url",
    "customAlias": "example"
  }'
```

### Test Redirect
```bash
curl -L https://your-app.onrender.com/[shortCode]
```

---

## ⚙️ Configuration Details

### Free Tier Limitations

Render's free tier includes:
- ✅ **512 MB RAM** - Our app is optimized for this
- ✅ **PostgreSQL database** with 1GB storage
- ✅ **Automatic HTTPS**
- ✅ **Automatic deploys** from Git
- ⚠️ **Spins down after 15 minutes** of inactivity (cold starts ~30 seconds)
- ❌ **No Redis** (we use in-memory fallback)

### JVM Memory Configuration

The application is configured with:
```bash
-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0
```

This ensures:
- Maximum heap: 512MB
- Initial heap: 256MB
- Container-aware memory management
- Uses 75% of available RAM

### Database Connection Pool

Optimized for free tier:
- **Maximum pool size**: 5 connections
- **Minimum idle**: 2 connections
- **Connection timeout**: 30 seconds
- **Max lifetime**: 30 minutes

### Cache Strategy

Since Redis is not available on free tier:
- ✅ **In-memory cache** for URL mappings
- ✅ **ConcurrentHashMap** for click counts
- ✅ **Automatic fallback** from Redis
- ⚠️ **Cache clears** on restart (not persistent)

---

## 📊 Monitoring

### View Logs
```bash
# In Render Dashboard
Services → Your Service → Logs
```

### Health Endpoints

| Endpoint | Purpose |
|----------|---------|
| `/api/health` | Detailed health status |
| `/api/health/live` | Liveness probe |
| `/api/health/ready` | Readiness probe |
| `/actuator/health` | Spring Boot Actuator health |
| `/actuator/metrics` | Application metrics |

### Check Database Connection
```bash
# From Render Shell
psql $DATABASE_URL
```

---

## 🐛 Troubleshooting

### Issue: Application Won't Start

**Symptom**: "Failed to configure a DataSource"

**Solution**:
1. Check `DATABASE_URL` environment variable is set
2. Verify database is running (Render Dashboard → Database)
3. Check logs for connection errors
4. Ensure `DatabaseConfig.java` is converting URL format correctly

### Issue: Slow Response Times

**Symptom**: First request after inactivity is slow

**Solution**:
- This is expected on free tier (cold starts)
- Consider upgrading to paid tier for always-on service
- Or use a service like UptimeRobot to ping your app every 5 minutes

### Issue: Out of Memory Errors

**Symptom**: Application crashes with OOM errors

**Solution**:
1. Check JVM settings in `render.yaml`
2. Reduce connection pool size in `application-prod.properties`
3. Review application logs for memory leaks
4. Consider upgrading to higher tier

### Issue: Database Connection Timeout

**Symptom**: "Connection timeout" in logs

**Solution**:
1. Verify database is in the same region as web service
2. Check database credentials in environment variables
3. Increase `connection-timeout` in properties
4. Verify database isn't sleeping (free tier limitation)

---

## 🔒 Security Recommendations

### Before Production

1. **Change default admin password**:
   ```bash
   ADMIN_PASSWORD=your-secure-password-here
   ```

2. **Enable CORS** if using from web frontend:
   ```java
   @Configuration
   public class CorsConfig implements WebMvcConfigurer {
       @Override
       public void addCorsMappings(CorsRegistry registry) {
           registry.addMapping("/api/**")
                   .allowedOrigins("https://your-frontend.com")
                   .allowedMethods("GET", "POST", "PUT", "DELETE");
       }
   }
   ```

3. **Rate limiting** is already configured:
   - 10 URL creations per hour per IP
   - 100 redirects per minute per IP

4. **HTTPS** is automatic on Render ✅

---

## 📈 Scaling Considerations

### When to Upgrade from Free Tier

Consider upgrading if you need:
- ❌ No cold starts (always-on service)
- ❌ More than 512MB RAM
- ❌ Distributed caching (Redis)
- ❌ Multiple instances (load balancing)
- ❌ More than 1GB database storage

### Upgrade Path

1. **Starter Tier** ($7/month):
   - Always-on (no cold starts)
   - Same 512MB RAM

2. **Standard Tier** ($25/month):
   - 2GB RAM
   - Better performance
   - Can add Redis

---

## 🧪 Testing Deployment

### Load Testing
```bash
# Using Apache Bench
ab -n 1000 -c 10 https://your-app.onrender.com/api/health

# Using curl in loop
for i in {1..100}; do
  curl -X POST https://your-app.onrender.com/api/shorten \
    -H "Content-Type: application/json" \
    -d "{\"url\": \"https://example.com/test-$i\"}"
done
```

### Database Verification
```bash
# Check tables were created
psql $DATABASE_URL -c "\dt"

# Check data
psql $DATABASE_URL -c "SELECT COUNT(*) FROM urls;"
```

---

## 📚 Additional Resources

- [Render Documentation](https://render.com/docs)
- [Spring Boot on Render](https://render.com/docs/deploy-spring-boot)
- [PostgreSQL on Render](https://render.com/docs/databases)
- [Render Blueprint Specification](https://render.com/docs/blueprint-spec)

---

## 🆘 Support

If you encounter issues:

1. Check Render logs (Dashboard → Logs)
2. Review application logs
3. Check database connectivity
4. Verify environment variables
5. Test health endpoints

**Common URLs**:
- Dashboard: https://dashboard.render.com
- Logs: https://dashboard.render.com/[service-id]/logs
- Metrics: https://your-app.onrender.com/actuator/metrics

---

## ✅ Deployment Checklist

- [ ] PostgreSQL database created
- [ ] Environment variables configured
- [ ] `BASE_URL` set to your Render URL
- [ ] `ADMIN_PASSWORD` changed from default
- [ ] Build completes successfully
- [ ] Health check returns 200 OK
- [ ] Can create short URLs
- [ ] Redirects work correctly
- [ ] Database persists data after restart
- [ ] Logs show no errors

---

**Status**: Ready for Render Free Tier Deployment! 🚀
