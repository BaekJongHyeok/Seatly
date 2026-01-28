package kr.jiyeok.seatly.di

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import com.russhwolf.settings.Settings
import kr.jiyeok.seatly.util.AppSettings
import kr.jiyeok.seatly.data.remote.ApiService
import kr.jiyeok.seatly.data.repository.SeatlyRepository
import kr.jiyeok.seatly.data.repository.SeatlyRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kr.jiyeok.seatly.domain.usecase.*
import kr.jiyeok.seatly.presentation.viewmodel.*
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val dataModule = module {
    single(named("ioDispatcher")) { Dispatchers.Default }
    single<Settings> { Settings() }
    single { AppSettings(get<Settings>()) }
    single {
        Json {
            ignoreUnknownKeys = true
            prettyPrint = true
            isLenient = true
        }
    }

    single {
        HttpClient {
            install(ContentNegotiation) {
                json(get<Json>())
            }
            install(Logging) {
                level = LogLevel.ALL
                logger = Logger.DEFAULT
            }
            // You can add default headers or authentication plugins here
            defaultRequest {
                headers {
                    append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }
            }
        }
    }

    single { ApiService(get()) }
    single<SeatlyRepository> { SeatlyRepositoryImpl(get(), Dispatchers.Default) }

    // UseCases
    factory { LoginUseCase(get(), Dispatchers.Default) }
    factory { LogoutUseCase(get(), Dispatchers.Default) }
    factory { GetUserInfoUseCase(get(), Dispatchers.Default) }
    factory { GetFavoriteCafesUseCase(get(), Dispatchers.Default) }
    factory { GetMyTimePassesUseCase(get(), Dispatchers.Default) }
    factory { GetCurrentSessions(get(), Dispatchers.Default) }
    factory { UpdateUserInfoUseCase(get(), Dispatchers.Default) }
    factory { DeleteAccountUseCase(get(), Dispatchers.Default) }
    factory { RegisterUseCase(get(), Dispatchers.Default) }
    factory { ChangePasswordUseCase(get(), Dispatchers.Default) }
    factory { GetUsersWithTimePassUseCase(get(), Dispatchers.Default) }
    factory { GetUserInfoAdminUseCase(get(), Dispatchers.Default) }
    factory { AddUserTimePassUseCase(get(), Dispatchers.Default) }
    factory { GetSessionsUseCase(get(), Dispatchers.Default) }
    factory { StartSessionUseCase(get(), Dispatchers.Default) }
    factory { EndSessionUseCase(get(), Dispatchers.Default) }
    factory { AssignSeatUseCase(get(), Dispatchers.Default) }
    factory { AutoAssignSeatUseCase(get(), Dispatchers.Default) }
    factory { GetStudyCafesUseCase(get(), Dispatchers.Default) }
    factory { GetCafeDetailUseCase(get(), Dispatchers.Default) }
    factory { CreateCafeUseCase(get(), Dispatchers.Default) }
    factory { UpdateCafeUseCase(get(), Dispatchers.Default) }
    factory { DeleteCafeUseCase(get(), Dispatchers.Default) }
    factory { AddFavoriteCafeUseCase(get(), Dispatchers.Default) }
    factory { RemoveFavoriteCafeUseCase(get(), Dispatchers.Default) }
    factory { GetCafeUsageUseCase(get(), Dispatchers.Default) }
    factory { DeleteUserTimePassUseCase(get(), Dispatchers.Default) }
    factory { GetAdminCafesUseCase(get(), Dispatchers.Default) }
    factory { GetCafeSeatsUseCase(get(), Dispatchers.Default) }
    factory { CreateSeatsUseCase(get(), Dispatchers.Default) }
    factory { UpdateSeatsUseCase(get(), Dispatchers.Default) }
    factory { DeleteSeatUseCase(get(), Dispatchers.Default) }
    factory { UploadImageUseCase(get(), Dispatchers.Default) }
    factory { GetImageUseCase(get(), Dispatchers.Default) }
    factory { DeleteImageUseCase(get(), Dispatchers.Default) }
}

val viewModelModule = module {
    factory { AuthViewModel(get(), get(), get()) }
    viewModel { HomeViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { SearchViewModel(get(), get(), get(), get(), get()) }
    viewModel { MyPageViewModel(get(), get(), get(), get(), get(), get(), get(named("ioDispatcher"))) }
    viewModel { AdminHomeViewModel(get(), get(), get(), get(named("ioDispatcher"))) }
    viewModel { EditProfileViewModel(get(), get(), get(), get(), get(), get(named("ioDispatcher"))) }
    viewModel { CafeFormViewModel(get(), get(), get(), get(), get(named("ioDispatcher"))) }
    viewModel { CafeDetailViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { AdminCafeDetailViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(named("ioDispatcher"))) }
}

fun initKoin() {
    startKoin {
        modules(dataModule, viewModelModule)
    }
}
