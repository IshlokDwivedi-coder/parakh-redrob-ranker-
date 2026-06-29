package com.parakh.scoring;

import java.util.Set;

/**
 * All the word lists for PARAKH in one place. Every keyword set, company roster, and the cities
 * the JD cares about live here, taken straight from the job description.
 *
 * The idea is to keep the judgement calls in this one file and out of the scorers, so when we
 * want to re-tune, this is the only place we have to touch.
 */
public final class Lexicon {

    private Lexicon() {}

    // ---- Titles that ARE the job: ranking / retrieval / search / recsys / applied-ML at scale ----
    public static final Set<String> BULLSEYE_TITLES = Set.of(
            "recommendation", "recsys", "ranking", "relevance", "search engineer", "search relevance",
            "information retrieval", "ml engineer", "machine learning engineer", "applied scientist",
            "applied ml", "ai engineer", "nlp engineer", "nlp scientist", "ai/ml", "personalization");

    // ---- Adjacent titles: real engineering, ML-credible, but not bullseye (data/research/SWE) ----
    public static final Set<String> ADJACENT_TITLES = Set.of(
            "data scientist", "research engineer", "research scientist", "data engineer",
            "software engineer", "backend engineer", "platform engineer", "mlops", "data science");

    // ---- Titles the keyword-trap candidates wear: skill list full of AI words, job is unrelated ----
    public static final Set<String> OFFTRACK_TITLES = Set.of(
            "marketing", "hr ", "human resource", "recruit", "talent acquisition", "sales", "account",
            "accountant", "finance", "operations manager", "customer support", "customer success",
            "civil engineer", "mechanical engineer", "graphic designer", "content writer", "project manager",
            "business analyst", "qa engineer", "quality assurance", "devops", "frontend", "front-end",
            "front end", "mobile developer", "android developer", "ios developer", ".net developer",
            "cloud engineer", "network engineer", "product manager", "program manager", "ui/ux");

    // ---- Core IR/ML evidence terms we want to see in CAREER DESCRIPTIONS, not just the skills list ----
    public static final Set<String> CORE_EVIDENCE_TERMS = Set.of(
            "embedding", "embeddings", "retrieval", "ranking", "rank ", "learning to rank", "ltr",
            "recommendation", "recommender", "recsys", "semantic search", "vector search", "vector db",
            "hybrid search", "bm25", "information retrieval", "ndcg", "mrr", "map@", "relevance",
            "personalization", "personalisation", "matching", "search quality", "candidate generation",
            "two-tower", "faiss", "pinecone", "weaviate", "qdrant", "milvus", "opensearch",
            "elasticsearch", "sentence-transformer", "sentence transformers", "bge", "e5 ", "rag",
            "sbert", "bert", "transformer", "fine-tun", "lora", "qlora", "peft", "re-rank", "reranking");

    // ---- Indian IT-services firms: services-only careers are an explicit disqualifier ----
    public static final Set<String> SERVICES_COMPANIES = Set.of(
            "tcs", "tata consultancy", "infosys", "wipro", "accenture", "cognizant", "capgemini",
            "tech mahindra", "hcl", "mindtree", "ltimindtree", "lti", "mphasis", "dxc", "hexaware",
            "larsen", "l&t infotech", "persistent systems", "birlasoft", "zensar", "coforge", "nttdata",
            "ntt data");

    // ---- Recognisable product companies (a signal, not exhaustive; we also check industry != IT Services) ----
    public static final Set<String> PRODUCT_COMPANIES = Set.of(
            "google", "meta", "facebook", "amazon", "microsoft", "apple", "netflix", "uber", "airbnb",
            "linkedin", "flipkart", "swiggy", "zomato", "ola", "razorpay", "cred", "phonepe", "paytm",
            "myntra", "meesho", "sharechat", "dream11", "navi", "groww", "zepto", "nvidia", "adobe",
            "salesforce", "atlassian", "stripe", "databricks", "openai", "anthropic", "walmart");

    // ---- CV / speech / robotics: down-weighted unless paired with NLP/IR ----
    public static final Set<String> CV_SPEECH_ROBOTICS = Set.of(
            "computer vision", "opencv", "image classification", "object detection", "segmentation",
            "speech recognition", "asr", "text-to-speech", "tts", "robotics", "slam", "lidar",
            "autonomous", "perception", "point cloud");

    // ---- Indian metros the JD names / will relocate-accept (Pune & Noida preferred) ----
    public static final Set<String> PREFERRED_CITIES = Set.of(
            "pune", "noida", "delhi", "new delhi", "ncr", "gurgaon", "gurugram", "ghaziabad",
            "faridabad", "mumbai", "bengaluru", "bangalore", "hyderabad", "chennai");

    /** true if the (already lower-cased) text contains any of the needles. */
    public static boolean containsAny(String haystackLower, Set<String> needles) {
        for (String n : needles) {
            if (haystackLower.contains(n)) return true;
        }
        return false;
    }

    /** how many distinct needles appear in the (already lower-cased) text. */
    public static int countDistinct(String haystackLower, Set<String> needles) {
        int c = 0;
        for (String n : needles) {
            if (haystackLower.contains(n)) c++;
        }
        return c;
    }
}
