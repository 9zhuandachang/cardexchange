#ifndef HM_SEPROJ_CARDPROP
#define HM_SEPROJ_CARDPROP

#include <opencv2/opencv.hpp>

//typedef unsigned __int32 card_id;
typedef __int32 card_id;    // for compatibility with Java, unsigned prefix is removed.

class CardProp
{
public:
    /// @brief Picure
    cv::Mat img;
    /// @brief Descriptor for the picture
    cv::Mat des;
    /// @brief ID of card. Got from serverlet side
    card_id id;

    CardProp() : CardProp(cv::Mat(), cv::Mat(), 0) {}
    explicit CardProp(const cv::Mat &des) : CardProp(cv::Mat(), des, 0) {}
    CardProp(const cv::Mat &img, const cv::Mat &des) : CardProp(img, des, 0) {}
    CardProp(const cv::Mat &des, int id) : CardProp(cv::Mat(), des, id) {}
    CardProp(const cv::Mat &img, const cv::Mat &des, int id) : img(img), des(des), id(id) {}
    // img-id and img only constructor not provided. usd cv::Mat() and 0 for img and id instead.

    ~CardProp()
    {
        this->img.deallocate();
        this->des.deallocate();
        return;
    }

    bool validate()
    {
        return (!(img.empty()) || !(des.empty()));
    }
};

#endif