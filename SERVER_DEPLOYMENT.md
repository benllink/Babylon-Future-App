# Babylon Future Server Deployment

This repository contains the Android client and a production-ready Flask backend package.

## Railway
1. Create a Railway project from this GitHub repository.
2. Add a persistent volume and mount it at `/data`.
3. Set environment variable `SECRET_KEY` to a long random value.
4. Railway builds the root `Dockerfile` automatically.
5. Generate a public domain for the service.
6. Open `/health` on the public domain and confirm `status: ok`.
7. Enter the Railway HTTPS domain in the Babylon Future Android app.

The SQLite database is stored at `/data/attendance.db`, so the mounted volume must remain attached.

Default first admin:
- Username: `admin`
- Password: `Admin@123`

Change the admin password after first deployment.
