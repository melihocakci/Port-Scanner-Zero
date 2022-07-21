#include <jni.h>
#include <string>
#include <netdb.h>
#include <arpa/inet.h>

extern "C" JNIEXPORT jstring JNICALL
Java_tr_edu_yildiz_portscanner_ScanActivity_getServByPort(JNIEnv* env, jobject, jint num)
{
    int portnum = (int) num;
    struct servent* res = getservbyport(htons(portnum), "tcp");
    std::string serv;
    if(res == nullptr) {
        serv = "unknown";
    } else {
        serv = res->s_name;
    }
    return env->NewStringUTF(serv.c_str());
}