package com.atcumt.common.utils;

public class HeatScoreUtil {
    public static double getPostHeat(int likeCount, int dislikeCount, int commentCount,
                                     double likeWeight, double commentWeight) {
        // 点赞差值
        int likeDifference = likeCount - dislikeCount;

        // 热度计算
        return likeWeight * likeDifference + commentWeight * commentCount;
    }

    public static double getPostHeat(int likeCount, int dislikeCount, int commentCount) {
        return getPostHeat(likeCount, dislikeCount, commentCount, 2.0, 1.0);
    }

    public static double getCommentHeat(int likeCount, int dislikeCount, int replyCount, long publishTime,
                                        double likeWeight, double replyWeight, double timeWeight, double timeScale) {
        // 点赞差值
        int likeDifference = likeCount - dislikeCount;

        // 时间部分（缩放时间戳）
        double timeComponent = publishTime / timeScale;
        if (timeComponent < 1) {
            timeComponent = 1;
        }

        timeComponent = timeWeight * Math.log10(timeComponent);

        // 热度计算
        return likeWeight * likeDifference + replyWeight * replyCount + timeComponent;
    }

    public static double getCommentHeat(int likeCount, int dislikeCount, int replyCount, long publishTime) {
        return getCommentHeat(likeCount, dislikeCount, replyCount, publishTime, 2.0, 1.0, 0.1, 1e9);
    }

    public static double getReplyHeat(int likeCount, int dislikeCount, long publishTime,
                                      double likeWeight, double timeWeight, double timeScale) {
        // 点赞差值
        int likeDifference = likeCount - dislikeCount;

        // 时间部分（缩放时间戳）
        double timeComponent = publishTime / timeScale;
        if (timeComponent < 1) {
            timeComponent = 1;
        }

        timeComponent = timeWeight * Math.log10(timeComponent);

        // 热度计算
        return likeWeight * likeDifference - timeComponent;
    }

    public static double getReplyHeat(int likeCount, int dislikeCount, long publishTime) {
        return getReplyHeat(likeCount, dislikeCount, publishTime, 2.0, 0.1, 1e9);
    }

    public static double getReplyHeat(long publishTime) {
        return getReplyHeat(0, 0, publishTime, 2.0, 0.1, 1e9);
    }
}
