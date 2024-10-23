ALTER TABLE article ADD COLUMN ArticleIntroSummary text;
CREATE index ON article(ArticleIntroSummary);
