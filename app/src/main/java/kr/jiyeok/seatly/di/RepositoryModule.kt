package kr.jiyeok.seatly.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kr.jiyeok.seatly.data.repository.AdminRepository
import kr.jiyeok.seatly.data.repository.AdminRepositoryImpl
import kr.jiyeok.seatly.data.repository.LoginRepository
import kr.jiyeok.seatly.data.repository.LoginRepositoryImpl
import kr.jiyeok.seatly.data.repository.ReservationRepository
import kr.jiyeok.seatly.data.repository.ReservationRepositoryImpl
import kr.jiyeok.seatly.data.repository.SessionRepository
import kr.jiyeok.seatly.data.repository.SessionRepositoryImpl
import kr.jiyeok.seatly.data.repository.StudyCafeRepository
import kr.jiyeok.seatly.data.repository.StudyCafeRepositoryImpl
import kr.jiyeok.seatly.data.repository.UserRepository
import kr.jiyeok.seatly.data.repository.UserRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAdminRepository(impl: AdminRepositoryImpl): AdminRepository

    @Binds
    @Singleton
    abstract fun bindLoginRepository(impl: LoginRepositoryImpl): LoginRepository

    @Binds
    @Singleton
    abstract fun bindReservationRepository(impl: ReservationRepositoryImpl): ReservationRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

    @Binds
    @Singleton
    abstract fun bindStudyCafeRepository(impl: StudyCafeRepositoryImpl): StudyCafeRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
}
