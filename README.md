# URL Shortener

A production-ready URL shortening service built with Spring Boot, featuring analytics, rate limiting, and caching.

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Database-blue.svg)](https://www.postgresql.org/)

## Features

- **URL Shortening** - Convert long URLs to short, shareable links with optional custom codes
- **Click Analytics** - Track clicks by date, referrer, and device type
- **Rate Limiting** - Configurable limits per IP address
- **Caching** - Redis support with in-memory fallback
- **Health Checks** - Built-in endpoints for monitoring

## Quick Start

### Prerequisites

- Java 21+
- PostgreSQL 12+
- Maven 3.9+
- Redis 6+ (optional)

### Setup

1. **Clone and configure**
   ```bash
   git clone https://github.com/yourusername/urlshortener.git
   cd urlshortener
   cp .env.example .env
   # Edit .env with your database credentials
   ```

2. **Create database**
   ```bash
   psql -U postgres -c "CREATE DATABASE urlshortener;"
   ```

3. **Run**
   ```bash
   ./mvnw spring-boot:run
   ```

4. **Test**
   ```bash
   # Health check
   curl http://localhost:8080/api/health
   
   # Create short URL
   curl -X POST http://localhost:8080/api/shorten \
     -H "Content-Type: application/json" \
     -d '{"url": "https://www.google.com"}'
   ```

## API Reference

### Create Short URL
```http
POST /api/shorten
Content-Type: application/json

{"url": "https://example.com/long-url", "customCode": "my-link"}
```

**Response:**
```json
{
  "originalUrl": "https://example.com/long-url",
  "shortUrl": "http://localhost:8080/my-link",
  "shortCode": "my-link"
}
```

### Redirect
```http
GET /{shortCode}
```
Returns `301 Moved Permanently` redirect to the original URL.

### Get Statistics
```http
GET /api/stats/{shortCode}
```

**Response:**
```json
{
  "shortCode": "my-link",
  "originalUrl": "https://example.com/...",
  "totalClicks": 1247,
  "clicksByDate": [{"date": "2024-01-15", "clicks": 145}],
  "topReferrers": [{"referrer": "https://twitter.com", "clicks": 456}],
  "deviceStats": [{"device": "Mobile", "clicks": 678}]
}
```

### List All URLs
```http
GET /api/urls
```

### Health Check
```http
GET /api/health
```

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DATABASE_URL` | PostgreSQL connection URL | `jdbc:postgresql://localhost:5432/urlshortener` |
| `DB_USERNAME` | Database username | `postgres` |
| `DB_PASSWORD` | Database password | `postgres` |
| `BASE_URL` | Base URL for short links | `http://localhost:8080` |
| `REDIS_HOST` | Redis host (optional) | `localhost` |
| `REDIS_PORT` | Redis port | `6379` |

### Rate Limiting

Configure in `application.properties`:
```properties
rate.limit.shorten.capacity=10                    # Max requests per period
rate.limit.shorten.refill-duration-minutes=60    # Period in minutes
rate.limit.redirect.capacity=100
rate.limit.redirect.refill-duration-minutes=1
```

## Deployment

### Docker

```bash
docker build -t urlshortener:latest .
docker run -p 8080:8080 \
  -e DATABASE_URL=jdbc:postgresql://host:5432/urlshortener \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=yourpassword \
  urlshortener:latest
```

### Render.com

1. Push code to GitHub
2. Create a new **Web Service** on Render
3. Connect your repository
4. Configure:
   - **Build Command:** `./mvnw clean package -DskipTests`
   - **Start Command:** `java -Dserver.port=$PORT -Dspring.profiles.active=prod -jar target/urlshortener-0.0.1-SNAPSHOT.jar`
5. Add environment variables:
   - `DATABASE_URL` - Create a PostgreSQL database on Render and use the Internal URL
   - `BASE_URL` - Your Render app URL (e.g., `https://your-app.onrender.com`)
6. Deploy

**Note:** Free tier spins down after 15 minutes of inactivity. First request after that takes ~30 seconds.

### Heroku

```bash
heroku create your-app-name
heroku addons:create heroku-postgresql:mini
heroku config:set BASE_URL=https://your-app-name.herokuapp.com
git push heroku main
```

## Architecture

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       ▼
┌─────────────────────────────────┐
│   Controllers (API Layer)       │
│   • UrlController               │
│   • HealthController            │
└──────────────┬──────────────────┘
               ▼
┌─────────────────────────────────┐
│   Services (Business Logic)     │
│   • UrlService                  │
│   • AnalyticsService            │
│   • CacheService                │
│   • RateLimitService            │
└──────────────┬──────────────────┘
         ┌─────┴─────┐
         ▼           ▼
   ┌──────────┐ ┌──────────┐
   │  Redis   │ │PostgreSQL│
   │ (Cache)  │ │(Primary) │
   └──────────┘ └──────────┘
```

## Tech Stack

- **Framework:** Spring Boot 3.x, Java 21
- **Database:** PostgreSQL with Spring Data JPA
- **Caching:** Redis (optional, with in-memory fallback)
- **Security:** Spring Security, Bucket4j rate limiting
- **Build:** Maven

## License

MIT License - see LICENSE file for details.

## Contact

**Maintainer:** Adewunmi  
**Email:** adewunmi7576@gmail.com  
**GitHub:** [@CrownAlter](https://github.com/CrownAlter)
