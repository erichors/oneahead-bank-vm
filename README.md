# OneAhead Bank VM Edition

A 3-tier VM version of OneAhead Bank with the React frontend preserved and the backend services running as Java Spring Boot apps.

For reusable AI/handoff context, see [docs/AI_CONTEXT.md](docs/AI_CONTEXT.md). It explains the architecture, runtime behavior, problem-pattern model, and a reusable prompt for working on this app elsewhere.

## Tiers and VM Sizes

| VM | Tier | Default port | Demo size | Safer demo size | Storage |
| --- | --- | ---: | --- | --- | --- |
| Frontend VM | React frontend | 8081 | 1 vCPU / 1 GB RAM | 1 vCPU / 2 GB RAM | 10 GB minimum, 20 GB comfortable |
| Backend VM | Java backend + PostgreSQL database | 8082 | 2 vCPU / 4 GB RAM | 2-4 vCPU / 8 GB RAM | 20 GB minimum, 40 GB comfortable |
| Credit VM | Java credit service | 8084 | 1 vCPU / 1-2 GB RAM | 2 vCPU / 2 GB RAM | 10 GB minimum, 20 GB comfortable |

Storage notes:

- The frontend VM mainly stores Node/npm dependencies, the React build, logs, and OS packages. Use 20 GB if you will build the frontend on the VM.
- The backend VM needs the most storage because it owns the local PostgreSQL data directory, application logs, and Java build artifacts. Use 40 GB if you plan to leave traffic running for long demos.
- The credit VM is stateless in this version, so 10 GB is enough for the app and OS. Use 20 GB for easier patching, logs, and troubleshooting.
- For short local demos, all three VMs can use the minimum values. For observability demos with retained logs and load generation, use the comfortable values.

Request path:

```text
Browser -> frontend VM -> backend VM + PostgreSQL -> credit service VM
```

## Build

Requires Java 17, Maven, Node.js, and npm.

```bash
mvn clean package
cd frontend
npm install
npm run build
```

## Run Locally

Start each service in its own terminal from this repo:

```bash
./scripts/run-credit-service.sh
./scripts/run-backend.sh
./scripts/run-frontend.sh
```

Open http://localhost:8081.

The scripts default to localhost, but each host can be changed with environment variables:

```bash
CREDIT_HOST=54.175.159.199 ./scripts/run-backend.sh
BACKEND_HOST=34.238.42.210 ./scripts/run-frontend.sh
```

For EC2, copy `scripts/ec2.env.example`, edit the IPs, then source it before starting a tier:

```bash
source scripts/ec2.env.example
./scripts/run-backend.sh
```

Demo users are seeded on backend startup:

| User | Location | Username | Password |
| --- | --- | --- | --- |
| Thomas Brady | Miami, Florida | `tbrady` | `goat` |
| Dave Morgan | Naperville, Illinois | `dmorgan` | `ahead1` |
| Matt Lowe | Cleveland, Ohio | `mlowe` | `ahead1` |
| Dipen Shah | Edison, New Jersey | `dshah` | `ahead1` |

## Deploy On Three VMs

This app is designed for three VMs:

- Frontend VM: builds and serves the React app.
- Backend VM: runs the Java backend and owns the local PostgreSQL database.
- Credit VM: runs the Java credit service.

The default database is local PostgreSQL on the backend VM. To switch to managed PostgreSQL such as Amazon RDS, keep the same app code and override the backend datasource environment variables.

The simplest deployment model is to clone this repo on all three VMs, build on the VM, and run only the tier assigned to that VM. For a cleaner production-style demo, build artifacts once and copy only the needed files to each VM.

### VM Prerequisites

Install Java 17 and Maven on the backend and credit VMs. Install PostgreSQL on the backend VM. Install Node.js, npm, and nginx on the frontend VM.

Ubuntu example:

```bash
sudo apt-get update
sudo apt-get install -y openjdk-17-jdk maven nodejs npm nginx git curl postgresql postgresql-contrib
```

Amazon Linux 2023 example:

```bash
sudo dnf update -y
sudo dnf install -y java-17-amazon-corretto-devel maven nodejs npm nginx git curl postgresql15 postgresql15-server
```

### Local PostgreSQL Setup

On the backend VM, initialize and start PostgreSQL.

Amazon Linux 2023:

```bash
sudo postgresql-setup --initdb
sudo systemctl enable --now postgresql
```

Ubuntu:

```bash
sudo systemctl enable --now postgresql
```

Create the demo database and user:

```bash
sudo -u postgres psql <<'SQL'
CREATE USER oneahead WITH PASSWORD 'oneahead';
CREATE DATABASE oneahead OWNER oneahead;
GRANT ALL PRIVILEGES ON DATABASE oneahead TO oneahead;
SQL
```

The backend defaults to this local database:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/oneahead
SPRING_DATASOURCE_USERNAME=oneahead
SPRING_DATASOURCE_PASSWORD=oneahead
SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver
```

You can also source `scripts/postgres-local.env.example` before starting the backend.

For managed PostgreSQL, copy `scripts/postgres-managed.env.example`, replace `YOUR_RDS_ENDPOINT` and the password, then source it on the backend VM before starting the backend. For RDS, allow inbound TCP `5432` from the backend EC2 security group only.

### Build The Java Services

On the backend VM and credit VM:

```bash
git clone https://github.com/YOUR_GITHUB_USER/oneahead-bank-vm.git
cd oneahead-bank-vm
mvn clean package
```

Credit VM:

```bash
PORT=8084 java -jar credit-service/target/banking-credit-service-1.0.0.jar
```

Backend VM:

```bash
PORT=8082 \
CREDIT_SERVICE_URL=http://CREDIT_VM_HOST:8084/api/credit/check \
java -jar backend/target/banking-backend-1.0.0.jar
```

Frontend VM development server:

```bash
cd frontend
PORT=8081 REACT_APP_API_URL=http://BACKEND_VM_HOST:8082 npm start
```

For a production static frontend, build with the backend URL baked in:

```bash
cd frontend
REACT_APP_API_URL=http://BACKEND_VM_HOST:8082 npm run build
```

Then serve `frontend/build` with nginx, Apache, or any static file server.

### Production-Style Frontend With nginx

On the frontend VM:

```bash
git clone https://github.com/YOUR_GITHUB_USER/oneahead-bank-vm.git
cd oneahead-bank-vm/frontend
npm install
REACT_APP_API_URL=http://BACKEND_VM_HOST:8082 npm run build
sudo mkdir -p /var/www/oneahead-bank
sudo cp -R build/* /var/www/oneahead-bank/
```

Create `/etc/nginx/conf.d/oneahead-bank.conf`:

```nginx
server {
    listen 8081;
    server_name _;

    root /var/www/oneahead-bank;
    index index.html;

    location / {
        try_files $uri /index.html;
    }
}
```

Restart nginx:

```bash
sudo nginx -t
sudo systemctl enable --now nginx
sudo systemctl restart nginx
```

### systemd Services

Create `/etc/systemd/system/oneahead-credit.service` on the credit VM:

```ini
[Unit]
Description=OneAhead Bank Credit Service
After=network-online.target
Wants=network-online.target

[Service]
WorkingDirectory=/home/ec2-user/oneahead-bank-vm
Environment=PORT=8084
ExecStart=/usr/bin/java -jar credit-service/target/banking-credit-service-1.0.0.jar
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

Create `/etc/systemd/system/oneahead-backend.service` on the backend VM:

```ini
[Unit]
Description=OneAhead Bank Backend
After=network-online.target
Wants=network-online.target

[Service]
WorkingDirectory=/home/ec2-user/oneahead-bank-vm
Environment=PORT=8082
Environment=CREDIT_SERVICE_URL=http://CREDIT_VM_HOST:8084/api/credit/check
Environment=SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/oneahead
Environment=SPRING_DATASOURCE_USERNAME=oneahead
Environment=SPRING_DATASOURCE_PASSWORD=oneahead
Environment=SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver
ExecStart=/usr/bin/java -jar backend/target/banking-backend-1.0.0.jar
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

Enable the service on the relevant VM:

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now oneahead-credit
sudo systemctl status oneahead-credit
```

For the backend VM, replace `oneahead-credit` with `oneahead-backend`.

## Deploy On AWS EC2

Recommended EC2 layout:

| EC2 instance | Suggested type | Storage | Open inbound ports |
| --- | --- | --- | --- |
| Frontend | `t3.small` | 20 GB gp3 | 22 from your IP, 8081 from your IP or demo audience |
| Backend + DB | `t3.medium` | 40 GB gp3 | 22 from your IP, 8082 from frontend security group |
| Credit | `t3.small` | 20 GB gp3 | 22 from your IP, 8084 from backend security group |

Use Amazon Linux 2023 or Ubuntu 22.04/24.04. Keep all three instances in the same VPC and subnet group. Public IPs are fine for a short demo, but security groups should still restrict app ports.

### EC2 Security Groups

Create three security groups:

- `oneahead-frontend-sg`
  - inbound TCP 22 from your IP
  - inbound TCP 8081 from your IP or demo audience
- `oneahead-backend-sg`
  - inbound TCP 22 from your IP
  - inbound TCP 8082 from `oneahead-frontend-sg`
- `oneahead-credit-sg`
  - inbound TCP 22 from your IP
  - inbound TCP 8084 from `oneahead-backend-sg`

Outbound can remain default allow-all for the demo.

### EC2 Setup Steps

1. Launch the three instances using the instance sizes and storage above.
2. SSH to each instance.
3. Install prerequisites using the Amazon Linux or Ubuntu commands in `VM Prerequisites`.
4. Clone the repo on each instance:

```bash
git clone https://github.com/YOUR_GITHUB_USER/oneahead-bank-vm.git
cd oneahead-bank-vm
```

5. On the credit instance:

```bash
mvn clean package
PORT=8084 java -jar credit-service/target/banking-credit-service-1.0.0.jar
```

6. On the backend instance, replace `CREDIT_PRIVATE_IP` with the credit instance private IP:

```bash
mvn clean package
PORT=8082 \
CREDIT_SERVICE_URL=http://CREDIT_PRIVATE_IP:8084/api/credit/check \
java -jar backend/target/banking-backend-1.0.0.jar
```

7. On the frontend instance, replace `BACKEND_PRIVATE_IP` with the backend instance private IP:

```bash
cd frontend
npm install
REACT_APP_API_URL=http://BACKEND_PRIVATE_IP:8082 npm run build
sudo mkdir -p /var/www/oneahead-bank
sudo cp -R build/* /var/www/oneahead-bank/
```

8. Configure nginx using the `Production-Style Frontend With nginx` section.
9. Open `http://FRONTEND_PUBLIC_IP:8081`.

### EC2 Smoke Tests

From the backend EC2 instance:

```bash
curl http://CREDIT_PRIVATE_IP:8084/api/credit/health
curl http://localhost:8082/actuator/health
```

From the frontend EC2 instance:

```bash
curl http://BACKEND_PRIVATE_IP:8082/api/account/balance
curl http://localhost:8081
```

From your laptop:

```bash
curl http://FRONTEND_PUBLIC_IP:8081
```

### EC2 Traffic Driver

Run traffic from your laptop, the frontend EC2 instance, or a separate utility host:

```bash
BACKEND_URL=http://BACKEND_PRIVATE_IP:8082 RATE=10 DURATION_SECONDS=600 ./scripts/drive-traffic.sh
```

For more concurrency during a demo:

```bash
BACKEND_URL=http://BACKEND_PRIVATE_IP:8082 RATE=20 DURATION_SECONDS=600 CONCURRENT=true ./scripts/drive-traffic.sh
```

## Controls Page

Open the controls with the settings gear in the top-right navigation.

The controls page can:

- Drive browser-generated load with a request-groups-per-second slider. It defaults to 3 requests per second and persists the last selected value in the browser.
- Enable 404 errors from the backend.
- Enable slow SQL simulation on deposit and transfer calls.
- Enable slow credit simulation before backend-to-credit calls.
- Enable the built-in backend CPU burn problem.

The UI load driver creates debit and credit activity across the seeded demo users. Debit examples include dinner, car payment, mortgage, groceries, gas, lunch, pet store, utilities, and other expenses with fake merchant names.

The first built-in problem pattern is `Backend CPU Burn`. It intentionally burns CPU for a configurable number of milliseconds on account and credit proxy requests. Add future problem patterns behind new `AdminConfig` keys in the backend.

## Headless Traffic Driver

Run traffic from your laptop or any VM:

```bash
BACKEND_URL=http://BACKEND_VM_HOST:8082 RATE=10 DURATION_SECONDS=600 ./scripts/drive-traffic.sh
```

`RATE` is request groups per second. Each group sends balance, deposit, transfer, and credit requests.

## Health Checks

```bash
curl http://localhost:8082/actuator/health
curl http://localhost:8084/actuator/health
```
