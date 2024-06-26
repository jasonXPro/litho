// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

#pragma once

#include <cstddef>
#include <type_traits>
#include "corefunctions.h"
#include "jni.h"

namespace facebook {
namespace flexlayout {
namespace jni {

/**
 * ScopedGlobalRef is a sort of smart reference that allows us to control the
 * lifespan of a JNI global reference.
 *
 * This class is designed so that when a ScopedGlobalRef goes out of scoped, its
 * destructor will delete -JNIEnv->DeleteGlobalRef()- the underlying JNI
 * reference.
 *
 * This class should be used to wrap all the global references we create during
 * normal JNI operations if we want reference to eventually go away (in JNI it
 * is a common operation to cache some global references throughout the lifespan
 * of a process, in which case using this class does not really make sense). The
 * idea behind this is that in JNI we should be very explicit about the lifespan
 * of global references. Global references can quickly get out of control if not
 * freed properly, and the developer should always be very aware of the lifespan
 * of each global reference that is created in JNI so that leaks are prevented.
 *
 * This class is very explicit in its behavior, and it does not allow to perform
 * unexpected conversions or unexpected ownership transfer. In practice, this
 * class acts as a unique pointer where the underying JNI reference can have one
 * and just one owner. Transfering ownership is allowed but it is an explicit
 * operation (implemneted via move semantics and also via explicity API calls).
 *
 * Note that this class doesn't receive an explicit JNIEnv at construction time.
 * At destruction time it uses vanillajni::getCurrentEnv() to retrieve the
 * JNIEnv.
 *
 * It is OK to cache a ScopedGlobalRef between different JNI native
 * method calls.
 */
template <typename T>
class ScopedGlobalRef {
  static_assert(
      (std::is_same<T, jclass>::value || std::is_same<T, jobject>::value ||
       std::is_same<T, jstring>::value || std::is_same<T, jthrowable>::value ||
       std::is_same<T, jbyteArray>::value ||
       std::is_same<T, jintArray>::value ||
       std::is_same<T, jshortArray>::value ||
       std::is_same<T, jcharArray>::value ||
       std::is_same<T, jlongArray>::value ||
       std::is_same<T, jfloatArray>::value ||
       std::is_same<T, jdoubleArray>::value ||
       std::is_same<T, jobjectArray>::value ||
       std::is_same<T, jbooleanArray>::value),
      "ScopedGlobalRef instantiated for invalid type");

 public:
  /**
   * Constructs a ScopedGlobalRef with a JNI global reference.
   *
   * @param globalRef the global reference to wrap. Can be NULL.
   */
  ScopedGlobalRef(T globalRef) : mGlobalRef(globalRef) {}

  /**
   * Equivalent to ScopedGlobalRef(NULL)
   */
  explicit ScopedGlobalRef() : mGlobalRef(NULL) {}

  /**
   * Move construction is allowed.
   */
  ScopedGlobalRef(ScopedGlobalRef&& s) noexcept : mGlobalRef(s.release()) {}

  /**
   * Move assignment is allowed.
   */
  auto operator=(ScopedGlobalRef&& s) noexcept -> ScopedGlobalRef& {
    reset(s.release());
    return *this;
  }

  ~ScopedGlobalRef() {
    reset();
  }

  /**
   * Deletes the currently held reference and reassigns a new one to the
   * ScopedGlobalRef.
   */
  void reset(T ptr = NULL) {
    if (ptr != mGlobalRef) {
      if (mGlobalRef != NULL) {
        jni::getCurrentEnv()->DeleteGlobalRef(mGlobalRef);
      }
      mGlobalRef = ptr;
    }
  }

  /**
   * Makes this ScopedGlobalRef not own the underlying JNI global reference.
   * After calling this method, the ScopedGlobalRef will not delete the JNI
   * global reference when the ScopedGlobalRef goes out of scope.
   */
  auto release() -> T {
    T globalRef = mGlobalRef;
    mGlobalRef = NULL;
    return globalRef;
  }

  /**
   * Returns the underlying JNI global reference.
   */
  auto get() const -> T {
    return mGlobalRef;
  }

  /**
   * Returns true if the underlying JNI reference is not NULL.
   */
  operator bool() const {
    return mGlobalRef != NULL;
  }

  ScopedGlobalRef(const ScopedGlobalRef& ref) = delete;
  auto operator=(const ScopedGlobalRef& other) -> ScopedGlobalRef& = delete;

 private:
  T mGlobalRef;
};

template <typename T>
auto make_global_ref(T globalRef) -> ScopedGlobalRef<T> {
  return ScopedGlobalRef<T>(globalRef);
}

} // namespace jni
} // namespace flexlayout
} // namespace facebook
