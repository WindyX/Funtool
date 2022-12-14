//
// Created by hins0 on 2020/5/13,013.
//

#ifndef FUNTOOL_IMAGEUTIL_H
#define FUNTOOL_IMAGEUTIL_H

#ifdef __cplusplus
extern "C" {
#endif
void i420torgba(const unsigned char *src,
                const int width,
                const int height,
                unsigned char *dst);

#ifdef __cplusplus
}
#endif

#endif //FUNTOOL_IMAGEUTIL_H
