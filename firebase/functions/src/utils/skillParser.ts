/**
 * SkillQuant - Skill name normalization and parsing utilities
 */

// Canonical skill name mappings (aliases → normalized name)
const SKILL_ALIASES: Record<string, string> = {
  // Frontend
  "react.js": "React", "reactjs": "React", "react js": "React", "react": "React",
  "vue.js": "Vue.js", "vuejs": "Vue.js", "vue": "Vue.js",
  "angular": "Angular", "angular.js": "Angular",
  "next.js": "Next.js", "nextjs": "Next.js",
  "nuxt.js": "Nuxt.js", "nuxtjs": "Nuxt.js", "nuxt": "Nuxt.js",
  "svelte": "Svelte", "sveltekit": "Svelte",
  "remix": "Remix",
  "gatsby": "Gatsby",
  "astro": "Astro",
  "htmx": "HTMX",
  "html": "HTML/CSS", "html5": "HTML/CSS", "css": "HTML/CSS", "css3": "HTML/CSS",
  "tailwind": "Tailwind CSS", "tailwindcss": "Tailwind CSS",
  "sass": "Sass/SCSS", "scss": "Sass/SCSS",
  "webgl": "WebGL/Three.js", "three.js": "WebGL/Three.js", "threejs": "WebGL/Three.js",
  "d3.js": "D3.js", "d3": "D3.js",
  "electron": "Electron",
  "tauri": "Tauri",
  "storybook": "Storybook",
  "rxjs": "RxJS",
  "redux": "Redux",
  "zustand": "Zustand",
  "webassembly": "WebAssembly", "wasm": "WebAssembly",
  "pwa": "PWA", "progressive web app": "PWA",
  // Backend
  "node.js": "Node.js", "nodejs": "Node.js",
  "python": "Python", "py": "Python",
  "java": "Java", "jvm": "Java",
  "spring": "Spring Boot", "spring boot": "Spring Boot", "springboot": "Spring Boot",
  "go": "Go", "golang": "Go",
  "rust": "Rust", "rustlang": "Rust",
  "c#": "C#", "csharp": "C#",
  ".net": ".NET", "dotnet": ".NET", "asp.net": ".NET",
  "php": "PHP", "laravel": "Laravel",
  "ruby": "Ruby", "rails": "Ruby on Rails", "ruby on rails": "Ruby on Rails", "ror": "Ruby on Rails",
  "django": "Django",
  "fastapi": "FastAPI", "fast api": "FastAPI",
  "flask": "Flask",
  "graphql": "GraphQL", "graph ql": "GraphQL",
  "grpc": "gRPC",
  "elixir": "Elixir", "phoenix": "Elixir",
  "scala": "Scala",
  "c++": "C++", "cpp": "C++",
  "haskell": "Haskell",
  "clojure": "Clojure",
  "erlang": "Erlang",
  "lua": "Lua",
  "perl": "Perl",
  "cobol": "COBOL",
  "deno": "Deno",
  "bun": "Bun",
  "nestjs": "NestJS", "nest.js": "NestJS",
  "express": "Express.js", "express.js": "Express.js", "expressjs": "Express.js",
  "prisma": "Prisma",
  "drizzle": "Drizzle ORM",
  "trpc": "tRPC",
  "celery": "Celery",
  "rest": "REST APIs", "rest api": "REST APIs", "restful": "REST APIs",
  "microservices": "Microservices", "microservice": "Microservices",
  "rabbitmq": "RabbitMQ", "amqp": "RabbitMQ",
  "nats": "NATS",
  // Mobile
  "kotlin": "Kotlin", "android": "Kotlin",
  "swift": "Swift", "ios": "Swift", "swiftui": "SwiftUI",
  "flutter": "Flutter", "dart": "Flutter",
  "react native": "React Native", "react-native": "React Native", "rn": "React Native",
  "jetpack compose": "Jetpack Compose", "compose": "Jetpack Compose",
  "kotlin multiplatform": "Kotlin Multiplatform", "kmp": "Kotlin Multiplatform", "kmm": "Kotlin Multiplatform",
  // DevOps & Cloud
  "kubernetes": "Kubernetes", "k8s": "Kubernetes", "kube": "Kubernetes",
  "docker": "Docker", "containers": "Docker",
  "terraform": "Terraform", "tf": "Terraform", "iac": "Terraform",
  "aws": "AWS", "amazon web services": "AWS", "ec2": "AWS", "s3": "AWS",
  "azure": "Azure", "microsoft azure": "Azure",
  "gcp": "Google Cloud", "google cloud": "Google Cloud", "google cloud platform": "Google Cloud",
  "ansible": "Ansible",
  "jenkins": "Jenkins", "ci/cd": "CI/CD", "cicd": "CI/CD",
  "github actions": "GitHub Actions", "github ci": "GitHub Actions",
  "gitlab": "GitLab CI/CD", "gitlab ci": "GitLab CI/CD",
  "helm": "Helm", "helm charts": "Helm",
  "prometheus": "Prometheus", "grafana": "Grafana",
  "linux": "Linux", "ubuntu": "Linux", "centos": "Linux",
  "nginx": "Nginx",
  "pulumi": "Pulumi",
  "argocd": "ArgoCD", "argo": "ArgoCD",
  "istio": "Istio", "service mesh": "Istio",
  "consul": "Consul",
  "nomad": "Nomad",
  "git": "Git", "github": "Git", "version control": "Git",
  "serverless": "Serverless", "lambda": "AWS Lambda", "aws lambda": "AWS Lambda", "cloud functions": "Serverless",
  "cloudformation": "CloudFormation",
  "aws cdk": "AWS CDK", "cdk": "AWS CDK",
  "bicep": "Azure Bicep", "azure bicep": "Azure Bicep",
  "gke": "GKE", "google kubernetes engine": "GKE",
  "firebase": "Firebase",
  "supabase": "Supabase",
  "vercel": "Vercel",
  "cloudflare workers": "Cloudflare Workers", "cloudflare": "Cloudflare Workers",
  "datadog": "Datadog",
  "new relic": "New Relic",
  "vault": "HashiCorp Vault", "hashicorp vault": "HashiCorp Vault",
  // Data & Databases
  "postgresql": "PostgreSQL", "postgres": "PostgreSQL",
  "mongodb": "MongoDB", "mongo": "MongoDB",
  "mysql": "MySQL",
  "redis": "Redis", "redis streams": "Redis",
  "elasticsearch": "Elasticsearch", "elastic": "Elasticsearch", "elk": "Elasticsearch",
  "opensearch": "OpenSearch",
  "kafka": "Apache Kafka", "apache kafka": "Apache Kafka",
  "spark": "Apache Spark", "apache spark": "Apache Spark", "pyspark": "Apache Spark",
  "hadoop": "Hadoop", "hdfs": "Hadoop", "mapreduce": "Hadoop",
  "snowflake": "Snowflake",
  "databricks": "Databricks",
  "dbt": "dbt", "data build tool": "dbt",
  "airflow": "Apache Airflow", "apache airflow": "Apache Airflow",
  "cassandra": "Cassandra", "apache cassandra": "Cassandra",
  "dynamodb": "DynamoDB",
  "neo4j": "Neo4j", "graph database": "Neo4j",
  "sqlite": "SQLite",
  "clickhouse": "ClickHouse",
  "timescaledb": "TimescaleDB",
  "bigquery": "BigQuery", "big query": "BigQuery",
  "redshift": "Redshift", "aws redshift": "Redshift",
  "sql": "SQL", "structured query": "SQL",
  // AI/ML
  "machine learning": "Machine Learning", "ml": "Machine Learning", "deep learning": "Machine Learning", "ai": "Machine Learning",
  "tensorflow": "TensorFlow",
  "pytorch": "PyTorch",
  "llm": "LLMs/GPT", "gpt": "LLMs/GPT", "large language model": "LLMs/GPT", "chatgpt": "LLMs/GPT", "openai": "LLMs/GPT",
  "langchain": "LangChain",
  "nlp": "NLP", "natural language processing": "NLP",
  "computer vision": "Computer Vision", "opencv": "Computer Vision",
  "mlops": "MLOps", "ml ops": "MLOps",
  "hugging face": "Hugging Face", "huggingface": "Hugging Face", "transformers": "Hugging Face",
  "data science": "Data Science", "data scientist": "Data Science",
  "rag": "RAG", "retrieval augmented generation": "RAG",
  // Data Analytics & BI
  "data analyst": "Data Analytics", "data analytics": "Data Analytics", "analytics": "Data Analytics",
  "power bi": "Power BI", "powerbi": "Power BI",
  "tableau": "Tableau",
  "looker": "Looker",
  "excel": "Excel/VBA", "vba": "Excel/VBA",
  "r programming": "R", "r language": "R", "rstudio": "R",
  "pandas": "Pandas",
  "jupyter": "Jupyter", "jupyter notebook": "Jupyter",
  "julia": "Julia",
  "matlab": "MATLAB",
  "data engineer": "Data Engineering", "data engineering": "Data Engineering", "etl": "ETL Pipelines",
  "fivetran": "Fivetran",
  // Security
  "cybersecurity": "Cybersecurity", "cyber security": "Cybersecurity", "infosec": "Cybersecurity",
  "penetration testing": "Penetration Testing", "pentest": "Penetration Testing", "ethical hacking": "Penetration Testing",
  "soc": "SOC Analyst", "security operations": "SOC Analyst", "siem": "SOC Analyst",
  "cloud security": "Cloud Security", "aws security": "Cloud Security",
  "devsecops": "DevSecOps",
  "zero trust": "Zero Trust",
  "oauth": "OAuth/OIDC", "oidc": "OAuth/OIDC", "openid": "OAuth/OIDC",
  // Web3
  "blockchain": "Blockchain",
  "solidity": "Solidity", "smart contract": "Solidity",
  "web3": "Web3", "dapp": "Web3", "decentralized": "Web3",
  "ethereum": "Ethereum", "eth": "Ethereum",
  "defi": "DeFi", "decentralized finance": "DeFi",
  // Design
  "figma": "Figma",
  "ux design": "UX Design", "ux": "UX Design", "user experience": "UX Design",
  "ui design": "UI Design", "ui": "UI Design", "user interface": "UI Design",
  "sketch": "Sketch",
  "adobe xd": "Adobe XD", "xd": "Adobe XD",
  // Management
  "agile": "Agile/Scrum", "scrum": "Agile/Scrum", "scrum master": "Agile/Scrum",
  "product manager": "Product Management", "product management": "Product Management",
  "jira": "Jira", "atlassian": "Jira",
  // Testing
  "selenium": "Selenium", "webdriver": "Selenium",
  "cypress": "Cypress",
  "playwright": "Playwright",
  "jest": "Jest",
  "qa": "QA Engineering", "quality assurance": "QA Engineering", "test automation": "QA Engineering",
  // IoT & Game Dev & Low Code
  "iot": "IoT", "internet of things": "IoT",
  "embedded": "Embedded Systems", "firmware": "Embedded Systems", "microcontroller": "Embedded Systems",
  "arduino": "Arduino",
  "raspberry pi": "Raspberry Pi",
  "unity": "Unity", "unity3d": "Unity",
  "unreal": "Unreal Engine", "unreal engine": "Unreal Engine", "ue5": "Unreal Engine",
  "godot": "Godot",
  "salesforce": "Salesforce", "apex": "Salesforce", "soql": "Salesforce",
  "sap": "SAP", "abap": "SAP",
  "servicenow": "ServiceNow",
  "wordpress": "WordPress", "wp": "WordPress",
  "shopify": "Shopify Dev", "liquid": "Shopify Dev",
  "networking": "Networking", "cisco": "Networking", "ccna": "Networking",
  "dns": "DNS/CDN", "cdn": "DNS/CDN",
  "verilog": "Verilog/VHDL", "vhdl": "Verilog/VHDL",
};

/**
 * Normalize a single skill name to its canonical form
 */
export function normalizeSkillName(raw: string): string {
  const lower = raw.trim().toLowerCase();
  return SKILL_ALIASES[lower] || raw.trim();
}

/**
 * Extract skill names from a job description or title text.
 * Returns a deduplicated list of normalized skill names.
 */
export function extractSkills(text: string): string[] {
  const lower = text.toLowerCase();
  const found = new Set<string>();

  // Check all known aliases against the text
  for (const [alias, canonical] of Object.entries(SKILL_ALIASES)) {
    // Word-boundary check for short aliases, substring for longer ones
    if (alias.length <= 3) {
      const regex = new RegExp(`\\b${escapeRegex(alias)}\\b`, "i");
      if (regex.test(lower)) {
        found.add(canonical);
      }
    } else {
      if (lower.includes(alias)) {
        found.add(canonical);
      }
    }
  }

  return Array.from(found);
}

function escapeRegex(str: string): string {
  return str.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

/**
 * Categorize a skill into a domain
 */
export function categorizeSkill(skillName: string): string {
  const categories: Record<string, string[]> = {
    "Backend": ["Spring Boot", "Node.js", "Go", "Rust", "Python", "Django", "FastAPI", "Flask", "PHP", "Laravel", "Ruby", "Ruby on Rails", "Java", "C#", ".NET", "Elixir", "Scala", "C++", "C", "Haskell", "Clojure", "Erlang", "Lua", "Perl", "COBOL", "Deno", "Bun", "NestJS", "Express.js", "Prisma", "Drizzle ORM", "tRPC", "Celery", "REST APIs", "Microservices", "RabbitMQ", "NATS", "GraphQL", "gRPC"],
    "Frontend": ["React", "Vue.js", "Next.js", "Nuxt.js", "Angular", "TypeScript", "JavaScript", "Tailwind CSS", "Svelte", "Remix", "Gatsby", "Astro", "HTMX", "HTML/CSS", "Sass/SCSS", "WebGL/Three.js", "D3.js", "Electron", "Tauri", "Storybook", "RxJS", "Redux", "Zustand", "WebAssembly", "PWA", "Three.js"],
    "AI/ML": ["Machine Learning", "TensorFlow", "PyTorch", "LLMs/GPT", "LangChain", "NLP", "Computer Vision", "MLOps", "Hugging Face", "Data Science", "RAG"],
    "DevOps": ["Kubernetes", "Docker", "Terraform", "CI/CD", "Ansible", "Jenkins", "GitHub Actions", "GitLab CI/CD", "Helm", "Prometheus", "Grafana", "Linux", "Nginx", "Pulumi", "ArgoCD", "Istio", "Consul", "Nomad", "Git", "Datadog", "New Relic"],
    "Cloud": ["AWS", "Google Cloud", "Azure", "Serverless", "AWS Lambda", "CloudFormation", "AWS CDK", "Azure Bicep", "GKE", "Firebase", "Supabase", "Vercel", "Cloudflare Workers"],
    "Mobile": ["Kotlin", "Swift", "SwiftUI", "Flutter", "React Native", "Jetpack Compose", "Kotlin Multiplatform"],
    "Data": ["PostgreSQL", "MongoDB", "MySQL", "Redis", "Elasticsearch", "OpenSearch", "Apache Kafka", "Apache Spark", "Hadoop", "Snowflake", "Databricks", "dbt", "Apache Airflow", "Cassandra", "DynamoDB", "Neo4j", "SQLite", "ClickHouse", "TimescaleDB", "BigQuery", "Redshift", "Data Analytics", "Power BI", "Tableau", "Looker", "Excel/VBA", "SQL", "R", "Pandas", "Jupyter", "Julia", "MATLAB", "Data Engineering", "ETL Pipelines", "Fivetran"],
    "Web3": ["Blockchain", "Solidity", "Web3", "Ethereum", "DeFi"],
    "Security": ["Cybersecurity", "Penetration Testing", "SOC Analyst", "Cloud Security", "DevSecOps", "Zero Trust", "OAuth/OIDC", "HashiCorp Vault"],
    "Design": ["Figma", "UX Design", "UI Design", "Sketch", "Adobe XD"],
    "Management": ["Agile/Scrum", "Product Management", "Jira"],
    "Testing": ["Selenium", "Cypress", "Playwright", "Jest", "QA Engineering"],
    "IoT": ["IoT", "Embedded Systems", "Arduino", "Raspberry Pi", "Verilog/VHDL"],
    "GameDev": ["Unity", "Unreal Engine", "Godot"],
    "LowCode": ["Salesforce", "SAP", "ServiceNow", "WordPress", "Shopify Dev"],
    "Networking": ["Networking", "DNS/CDN"],
  };

  for (const [category, skills] of Object.entries(categories)) {
    if (skills.includes(skillName)) {
      return category;
    }
  }
  return "General";
}

